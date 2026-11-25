$ErrorActionPreference = "Stop"

function Login-User([string]$username, [string]$password) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $loginUrl = "http://localhost:8080/login"
    $body = @{
        username = $username
        password = $password
    }
    $resp = Invoke-WebRequest -Uri $loginUrl -Method POST -Body $body -WebSession $session -MaximumRedirection 5 -UseBasicParsing
    $dash = Invoke-WebRequest -Uri "http://localhost:8080/dashboard" -WebSession $session -UseBasicParsing
    if ($dash.StatusCode -ne 200 -or $dash.Content.Contains("Sign in to your account")) {
        throw "Login failed for $username!"
    }
    Write-Host "[AUTH] $username logged in successfully."
    return $session
}

Write-Host "===================================================="
Write-Host "STARTING END-TO-END QUIZ SUITE TESTS"
Write-Host "===================================================="

# 1. Login all actors
$facultySession  = Login-User "faculty"  "Faculty@123"
$student1Session = Login-User "student1" "Student@123"
$student2Session = Login-User "student2" "Student@123"
$adminSession    = Login-User "admin"    "Admin@123"

# TEST 1: Faculty creates and publishes a quiz
Write-Host "`n--- TEST 1: Faculty creates & publishes a quiz ---"
$startTime = (Get-Date).AddDays(-1).ToString("yyyy-MM-ddTHH:mm")
$endTime   = (Get-Date).AddDays(7).ToString("yyyy-MM-ddTHH:mm")
$createBody = @{
    courseId = "1"
    title = "E2E Timed Test Quiz Automated"
    durationMinutes = "15"
    startTime = $startTime
    endTime = $endTime
}
$createResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/create" -Method POST -Body $createBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Create Quiz POST Status: $($createResp.StatusCode)"

# Query latest quiz id from database
$quizIdStr = (mysql -u root -proot -N -e "SELECT quiz_id FROM rbac_db.quizzes WHERE title='E2E Timed Test Quiz Automated' ORDER BY quiz_id DESC LIMIT 1;").Trim()
$quizId = [int64]$quizIdStr
Write-Host "Created Quiz ID: $quizId"

# Add question to quiz
$qBody = @{
    questionText = "What is the worst-case time complexity of Merge Sort?"
    optionA = "O(n)"
    optionB = "O(n log n)"
    optionC = "O(n^2)"
    optionD = "O(1)"
    correctOption = "B"
    marks = "10"
}
$qResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions/add" -Method POST -Body $qBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Add Question POST Status: $($qResp.StatusCode)"

$questionIdStr = (mysql -u root -proot -N -e "SELECT question_id FROM rbac_db.questions WHERE quiz_id=$quizId LIMIT 1;").Trim()
$questionId = [int64]$questionIdStr
Write-Host "Created Question ID: $questionId"

# Publish quiz
$pubBody = @{ published = "true" }
$pubResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/publish" -Method POST -Body $pubBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Publish Quiz POST Status: $($pubResp.StatusCode)"

$isPub = (mysql -u root -proot -N -e "SELECT is_published FROM rbac_db.quizzes WHERE quiz_id=$quizId;").Trim()
Write-Host "Quiz is_published in DB: $isPub"
if ($isPub -ne "1") { throw "Quiz was not published!" }

# TEST 2: student1 starts quiz and requests 5 more minutes
Write-Host "`n--- TEST 2: student1 starts quiz & requests 5 more minutes ---"
$startResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/start" -Method POST -WebSession $student1Session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Student1 Start Quiz POST Status: $($startResp.StatusCode)"

# Verify attempt created
$att1Check = (mysql -u root -proot -N -e "SELECT attempt_id, extra_minutes FROM rbac_db.quiz_attempts WHERE quiz_id=$quizId AND student_id=(SELECT student_id FROM rbac_db.students WHERE user_id=(SELECT user_id FROM rbac_db.users WHERE username='student1'));").Trim()
Write-Host "Student1 Attempt in DB: $att1Check"

# Student1 requests 5 minutes extension
$reqBody = @{
    requestedMinutes = "5"
    reason = "Intermittent WiFi disruption during quiz"
    source = "take"
}
$reqResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/request-extension" -Method POST -Body $reqBody -WebSession $student1Session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Student1 Request Extension POST Status: $($reqResp.StatusCode)"

$reqStatus = (mysql -u root -proot -N -e "SELECT status, requested_minutes FROM rbac_db.quiz_extension_requests WHERE quiz_id=$quizId AND student_id=(SELECT student_id FROM rbac_db.students WHERE user_id=(SELECT user_id FROM rbac_db.users WHERE username='student1')) ORDER BY request_id DESC LIMIT 1;").Trim()
Write-Host "Student1 Request DB Status: $reqStatus"
if (-not $reqStatus.Contains("PENDING")) { throw "Request was not recorded as PENDING!" }

$requestIdStr = (mysql -u root -proot -N -e "SELECT request_id FROM rbac_db.quiz_extension_requests WHERE quiz_id=$quizId ORDER BY request_id DESC LIMIT 1;").Trim()
$requestId = [int64]$requestIdStr
Write-Host "Extension Request ID: $requestId"

# TEST 3: Faculty approves it
Write-Host "`n--- TEST 3: Faculty approves the request ---"
$apprResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/extensions/$requestId/approve" -Method POST -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Approve Extension POST Status: $($apprResp.StatusCode)"

$reqStatusAfter = (mysql -u root -proot -N -e "SELECT status FROM rbac_db.quiz_extension_requests WHERE request_id=$requestId;").Trim()
Write-Host "Request Status after approval: $reqStatusAfter"
if ($reqStatusAfter -ne "APPROVED") { throw "Request was not approved!" }

# TEST 4: student1's timer increases by 5 minutes, while another student's timer is unchanged
Write-Host "`n--- TEST 4: Verify student1 extra_minutes increased, student2 unchanged ---"
$stu1Extra = (mysql -u root -proot -N -e "SELECT extra_minutes FROM rbac_db.quiz_attempts WHERE quiz_id=$quizId AND student_id=(SELECT student_id FROM rbac_db.students WHERE user_id=(SELECT user_id FROM rbac_db.users WHERE username='student1'));").Trim()
Write-Host "Student1 DB extra_minutes: $stu1Extra"
if ($stu1Extra -ne "5") { throw "Student1 extra_minutes is not 5! Found: $stu1Extra" }

# Student2 starts quiz
$stu2Start = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/start" -Method POST -WebSession $student2Session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Student2 Start Quiz POST Status: $($stu2Start.StatusCode)"

$stu2Extra = (mysql -u root -proot -N -e "SELECT extra_minutes FROM rbac_db.quiz_attempts WHERE quiz_id=$quizId AND student_id=(SELECT student_id FROM rbac_db.students WHERE user_id=(SELECT user_id FROM rbac_db.users WHERE username='student2'));").Trim()
Write-Host "Student2 DB extra_minutes: $stu2Extra"
if ($stu2Extra -ne "0") { throw "Student2 extra_minutes was affected! Expected 0, found: $stu2Extra" }

# Verify HTML output of Student1 take page includes the increased timer / approved badge
$stu1TakeResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/take" -WebSession $student1Session -UseBasicParsing
$stu1Html = $stu1TakeResp.Content
$hasStu1ExtraBadge = $stu1Html.Contains("+5 extra mins granted") -or $stu1Html.Contains("Request Approved (+5m)")
Write-Host "Student1 take page displays approved extra time badge: $hasStu1ExtraBadge"

# TEST 5: A rejected request changes nothing
Write-Host "`n--- TEST 5: Rejected request changes nothing ---"
$req2Body = @{
    requestedMinutes = "10"
    reason = "Need more time to review answers"
    source = "take"
}
$req2Resp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/request-extension" -Method POST -Body $req2Body -WebSession $student2Session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Student2 Request Extension POST Status: $($req2Resp.StatusCode)"

$req2IdStr = (mysql -u root -proot -N -e "SELECT request_id FROM rbac_db.quiz_extension_requests WHERE quiz_id=$quizId ORDER BY request_id DESC LIMIT 1;").Trim()
$req2Id = [int64]$req2IdStr

# Faculty rejects request 2
$rejResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/extensions/$req2Id/reject" -Method POST -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Reject Extension POST Status: $($rejResp.StatusCode)"

$req2StatusAfter = (mysql -u root -proot -N -e "SELECT status FROM rbac_db.quiz_extension_requests WHERE request_id=$req2Id;").Trim()
Write-Host "Student2 Request Status after rejection: $req2StatusAfter"
if ($req2StatusAfter -ne "REJECTED") { throw "Request 2 was not rejected!" }

$stu2ExtraAfter = (mysql -u root -proot -N -e "SELECT extra_minutes FROM rbac_db.quiz_attempts WHERE quiz_id=$quizId AND student_id=(SELECT student_id FROM rbac_db.students WHERE user_id=(SELECT user_id FROM rbac_db.users WHERE username='student2'));").Trim()
Write-Host "Student2 extra_minutes after rejection: $stu2ExtraAfter (unchanged at 0)"
if ($stu2ExtraAfter -ne "0") { throw "Student2 extra_minutes changed after rejection!" }

# TEST 6: Faculty edits duration and end time
Write-Host "`n--- TEST 6: Faculty edits quiz duration and end time ---"
$newEndTime = (Get-Date).AddDays(14).ToString("yyyy-MM-ddTHH:mm")
$editBody = @{
    title = "E2E Timed Test Quiz Automated (Updated by Faculty)"
    durationMinutes = "25"
    startTime = $startTime
    endTime = $newEndTime
    isPublished = "true"
}
$editResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/edit" -Method POST -Body $editBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Faculty Edit Quiz POST Status: $($editResp.StatusCode)"

$dbQuizAfterEdit = (mysql -u root -proot -N -e "SELECT title, duration_minutes FROM rbac_db.quizzes WHERE quiz_id=$quizId;").Trim()
Write-Host "DB Quiz after edit: $dbQuizAfterEdit"
if (-not $dbQuizAfterEdit.Contains("Updated by Faculty") -or -not $dbQuizAfterEdit.Contains("25")) {
    throw "Quiz edit failed to update duration and title in DB!"
}

# TEST 7: Faculty cannot edit questions once an attempt exists
Write-Host "`n--- TEST 7: Faculty cannot edit or delete questions once attempts exist ---"
# Attempt to add question
$badAddBody = @{
    questionText = "Should this question be added after attempts?"
    optionA = "Yes"
    optionB = "No"
    optionC = "Maybe"
    optionD = "Never"
    correctOption = "B"
    marks = "5"
}
$badAddResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions/add" -Method POST -Body $badAddBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
$addBlocked = $badAddResp.Content.Contains("Cannot add questions: students have already attempted this quiz")
Write-Host "Adding question after attempt blocked: $addBlocked"
if (-not $addBlocked) { throw "Adding question after attempt was NOT blocked!" }

# Attempt to delete question
$badDelResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions/$questionId/delete" -Method POST -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
$delBlocked = $badDelResp.Content.Contains("Cannot delete questions: students have already attempted this quiz")
Write-Host "Deleting question after attempt blocked: $delBlocked"
if (-not $delBlocked) { throw "Deleting question after attempt was NOT blocked!" }

# Attempt to update question
$badEditBody = @{
    questionText = "Modified prompt"
    optionA = "A"
    optionB = "B"
    optionC = "C"
    optionD = "D"
    correctOption = "A"
    marks = "1"
}
$badEditResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions/$questionId/edit" -Method POST -Body $badEditBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
$editBlocked = $badEditResp.Content.Contains("Cannot edit questions: students have already attempted this quiz")
Write-Host "Editing question after attempt blocked: $editBlocked"
if (-not $editBlocked) { throw "Editing question after attempt was NOT blocked!" }

# TEST 8: Admin gets a 403 on all quiz URLs
Write-Host "`n--- TEST 8: Admin gets 403 on all quiz URLs ---"
$adminUrls = @(
    @{ Method = "GET";  Url = "http://localhost:8080/quizzes" },
    @{ Method = "GET";  Url = "http://localhost:8080/quizzes/manage" },
    @{ Method = "GET";  Url = "http://localhost:8080/quizzes/$quizId/edit" },
    @{ Method = "GET";  Url = "http://localhost:8080/quizzes/$quizId/results" },
    @{ Method = "GET";  Url = "http://localhost:8080/quizzes/$quizId/take" },
    @{ Method = "POST"; Url = "http://localhost:8080/quizzes/create"; Body = @{ courseId="1"; title="Admin Quiz"; durationMinutes="10"; startTime=$startTime; endTime=$endTime } },
    @{ Method = "POST"; Url = "http://localhost:8080/quizzes/$quizId/edit"; Body = @{ title="Admin Edit"; durationMinutes="10"; startTime=$startTime; endTime=$endTime } },
    @{ Method = "POST"; Url = "http://localhost:8080/quizzes/$quizId/start" },
    @{ Method = "POST"; Url = "http://localhost:8080/quizzes/$quizId/request-extension"; Body = @{ requestedMinutes="5"; reason="Admin test" } },
    @{ Method = "POST"; Url = "http://localhost:8080/quizzes/extensions/1/approve" }
)

$admin403Count = 0
foreach ($test in $adminUrls) {
    try {
        if ($test.Method -eq "GET") {
            $resp = Invoke-WebRequest -Uri $test.Url -Method GET -WebSession $adminSession -UseBasicParsing
        } else {
            $body = if ($test.Body) { $test.Body } else { @{} }
            $resp = Invoke-WebRequest -Uri $test.Url -Method POST -Body $body -WebSession $adminSession -UseBasicParsing
        }
        Write-Host "UNEXPECTED: $($test.Method) $($test.Url) returned $($resp.StatusCode)"
    } catch {
        $ex = $_.Exception
        if ($ex.Response -ne $null) {
            $code = [int]$ex.Response.StatusCode
            if ($code -eq 403) {
                Write-Host "[403 CONFIRMED] $($test.Method) $($test.Url) -> 403 Forbidden"
                $admin403Count++
            } else {
                Write-Host "Unexpected status $code on $($test.Method) $($test.Url)"
            }
        } else {
            Write-Host "Exception: $($ex.Message)"
        }
    }
}

Write-Host "`nAdmin 403 Tests Passed: $admin403Count / $($adminUrls.Count)"
if ($admin403Count -ne $adminUrls.Count) {
    throw "Not all admin quiz endpoints returned 403 Forbidden!"
}

Write-Host "`n===================================================="
Write-Host "ALL 8 TEST CASES PASSED SUCCESSFULLY!"
Write-Host "===================================================="
