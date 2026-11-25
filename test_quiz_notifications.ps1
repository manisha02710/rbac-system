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
Write-Host "STARTING QUIZ NOTIFICATIONS VERIFICATION TESTS"
Write-Host "===================================================="

# 1. Log in users
$facultySession  = Login-User "faculty"  "Faculty@123"
$student1Session = Login-User "student1" "Student@123"
$adminSession    = Login-User "admin"    "Admin@123"

# Clean up any past test quizzes
mysql -u root -proot -e "DELETE FROM rbac_db.quizzes WHERE title LIKE 'Notification Test Quiz%' OR title LIKE 'E2E Timed Test Quiz%';"

# STEP 1: Faculty publishes a quiz ending within 12 hours (Due Soon)
Write-Host "`n--- STEP 1: Faculty publishes a quiz (Due within 12 hours) ---"
$startTime = (Get-Date).AddHours(-1).ToString("yyyy-MM-ddTHH:mm")
$endTime   = (Get-Date).AddHours(12).ToString("yyyy-MM-ddTHH:mm") # < 24 hours -> Due soon!

$createBody = @{
    courseId = "1" # Data Structures (Computer Science)
    title = "Notification Test Quiz Alpha"
    durationMinutes = "15"
    startTime = $startTime
    endTime = $endTime
}
$createResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/create" -Method POST -Body $createBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Create Quiz Status: $($createResp.StatusCode)"

$quizIdStr = (mysql -u root -proot -N -e "SELECT quiz_id FROM rbac_db.quizzes WHERE title='Notification Test Quiz Alpha' ORDER BY quiz_id DESC LIMIT 1;").Trim()
$quizId = [int64]$quizIdStr
Write-Host "Created Quiz ID: $quizId"

# Add question
$qBody = @{
    questionText = "Which data structure follows FIFO order?"
    optionA = "Stack"
    optionB = "Queue"
    optionC = "Tree"
    optionD = "Graph"
    correctOption = "B"
    marks = "10"
}
$qResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions/add" -Method POST -Body $qBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Add Question Status: $($qResp.StatusCode)"

# Publish
$pubBody = @{ published = "true" }
$pubResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/publish" -Method POST -Body $pubBody -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Publish Quiz Status: $($pubResp.StatusCode)"

# STEP 2: student1 sees the banner, 'Due soon' badge, and the sidebar count
Write-Host "`n--- STEP 2: student1 checks Dashboard and Sidebar for Quiz Banner & Badge ---"
$stuDashResp = Invoke-WebRequest -Uri "http://localhost:8080/dashboard" -WebSession $student1Session -UseBasicParsing
$stuDashHtml = $stuDashResp.Content

# Check banner
$hasBanner = $stuDashHtml.Contains("New quiz: Notification Test Quiz Alpha")
Write-Host "Student1 Dashboard shows 'New quiz' banner: $hasBanner"
if (-not $hasBanner) { throw "Student1 Dashboard did not show the New Quiz banner!" }

# Check 'Due soon' highlight on banner
$hasDueSoonBanner = $stuDashHtml.Contains("Due soon")
Write-Host "Student1 Dashboard banner shows 'Due soon' badge: $hasDueSoonBanner"
if (-not $hasDueSoonBanner) { throw "Student1 Dashboard banner did not show the 'Due soon' badge!" }

# Check 'Take quiz' button link
$hasTakeQuizLink = $stuDashHtml.Contains("/quizzes/$quizId/take")
Write-Host "Student1 Dashboard banner has 'Take quiz' link: $hasTakeQuizLink"
if (-not $hasTakeQuizLink) { throw "Student1 Dashboard banner missing 'Take quiz' button link!" }

# Check 'View all quizzes' link
$hasViewAllLink = $stuDashHtml.Contains("View all quizzes") -and $stuDashHtml.Contains("/quizzes")
Write-Host "Student1 Dashboard shows 'View all quizzes' link: $hasViewAllLink"
if (-not $hasViewAllLink) { throw "Student1 Dashboard missing 'View all quizzes' link!" }

# Check sidebar count badge
$hasSidebarBadge = $stuDashHtml -match 'badge rounded-pill bg-danger ms-auto'
Write-Host "Student1 Sidebar shows unattempted quiz count badge: $hasSidebarBadge"
if (-not $hasSidebarBadge) { throw "Student1 Sidebar did not show the count badge!" }

# STEP 3: On student's Quizzes page, check NEW badge and Due soon badge
Write-Host "`n--- STEP 3: student1 checks Quizzes page for NEW and Due soon badges ---"
$stuQuizResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes" -WebSession $student1Session -UseBasicParsing
$stuQuizHtml = $stuQuizResp.Content

$hasNewBadge = $stuQuizHtml.Contains("NEW")
Write-Host "Quizzes page shows 'NEW' badge: $hasNewBadge"
if (-not $hasNewBadge) { throw "Quizzes page did not show 'NEW' badge!" }

$hasDueSoonQuizPage = $stuQuizHtml.Contains("Due soon")
Write-Host "Quizzes page shows 'Due soon' badge: $hasDueSoonQuizPage"
if (-not $hasDueSoonQuizPage) { throw "Quizzes page did not show 'Due soon' badge!" }

# STEP 4: Faculty and Admin see NO banner or badge
Write-Host "`n--- STEP 4: Verify Faculty and Admin see NO banner or badge ---"
$facDashResp = Invoke-WebRequest -Uri "http://localhost:8080/dashboard" -WebSession $facultySession -UseBasicParsing
$facDashHtml = $facDashResp.Content
$facHasBanner = $facDashHtml.Contains("New quiz: Notification Test Quiz Alpha")
$facHasBadge  = $facDashHtml -match 'badge rounded-pill bg-danger ms-auto'
Write-Host "Faculty sees New Quiz banner: $facHasBanner (Expected: False)"
Write-Host "Faculty sees Sidebar count badge: $facHasBadge (Expected: False)"
if ($facHasBanner -or $facHasBadge) { throw "Faculty saw quiz banner or badge!" }

$admDashResp = Invoke-WebRequest -Uri "http://localhost:8080/dashboard" -WebSession $adminSession -UseBasicParsing
$admDashHtml = $admDashResp.Content
$admHasBanner = $admDashHtml.Contains("New quiz: Notification Test Quiz Alpha")
$admHasBadge  = $admDashHtml -match 'badge rounded-pill bg-danger ms-auto'
Write-Host "Admin sees New Quiz banner: $admHasBanner (Expected: False)"
Write-Host "Admin sees Sidebar count badge: $admHasBadge (Expected: False)"
if ($admHasBanner -or $admHasBadge) { throw "Admin saw quiz banner or badge!" }

# STEP 5: student1 takes and submits the quiz; banner and count disappear
Write-Host "`n--- STEP 5: student1 submits the quiz; banner and count disappear ---"
# Start attempt
$startResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/start" -Method POST -WebSession $student1Session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Student1 Start Quiz Status: $($startResp.StatusCode)"

# Submit attempt
$subBody = @{
    "answer_$(mysql -u root -proot -N -e "SELECT question_id FROM rbac_db.questions WHERE quiz_id=$quizId LIMIT 1;").Trim()" = "B"
}
$subResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/submit" -Method POST -Body $subBody -WebSession $student1Session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Student1 Submit Quiz Status: $($subResp.StatusCode)"

# Re-check Dashboard: Banner must be gone, sidebar badge must be gone
$stuDashAfterResp = Invoke-WebRequest -Uri "http://localhost:8080/dashboard" -WebSession $student1Session -UseBasicParsing
$stuDashAfterHtml = $stuDashAfterResp.Content

$bannerGone = -not $stuDashAfterHtml.Contains("New quiz: Notification Test Quiz Alpha")
$badgeGone  = -not ($stuDashAfterHtml -match 'badge rounded-pill bg-danger ms-auto')
Write-Host "Student1 Dashboard banner disappeared after submit: $bannerGone"
Write-Host "Student1 Sidebar count badge disappeared after submit: $badgeGone"
if (-not $bannerGone) { throw "Banner did not disappear after student1 submitted the quiz!" }
if (-not $badgeGone) { throw "Sidebar count badge did not disappear after student1 submitted the quiz!" }

# STEP 6: Approved Extension shows update banner once
Write-Host "`n--- STEP 6: Approved Extension shows update banner once ---"
# Faculty publishes a second quiz for extension test
$create2Body = @{
    courseId = "1"
    title = "Notification Test Quiz Beta"
    durationMinutes = "15"
    startTime = $startTime
    endTime = (Get-Date).AddDays(5).ToString("yyyy-MM-ddTHH:mm")
}
$create2Resp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/create" -Method POST -Body $create2Body -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
$quiz2IdStr = (mysql -u root -proot -N -e "SELECT quiz_id FROM rbac_db.quizzes WHERE title='Notification Test Quiz Beta' ORDER BY quiz_id DESC LIMIT 1;").Trim()
$quiz2Id = [int64]$quiz2IdStr

$pub2Body = @{ published = "true" }
$pub2Resp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quiz2Id/publish" -Method POST -Body $pub2Body -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing

# student1 requests an extension on Quiz Beta
$extReqBody = @{
    requestedMinutes = "5"
    reason = "Network latency during test session"
    source = "index"
}
$extReqResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quiz2Id/request-extension" -Method POST -Body $extReqBody -WebSession $student1Session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Extension Request submitted: $($extReqResp.StatusCode)"

$reqIdStr = (mysql -u root -proot -N -e "SELECT request_id FROM rbac_db.quiz_extension_requests WHERE quiz_id=$quiz2Id ORDER BY request_id DESC LIMIT 1;").Trim()
$reqId = [int64]$reqIdStr
Write-Host "Extension Request ID: $reqId"

# Faculty approves the extension
$apprResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/extensions/$reqId/approve" -Method POST -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing
Write-Host "Faculty Approved Extension: $($apprResp.StatusCode)"

# student1 checks Dashboard -> should see update banner: "Your extension request for Notification Test Quiz Beta was approved"
$stuDashExtResp = Invoke-WebRequest -Uri "http://localhost:8080/dashboard" -WebSession $student1Session -UseBasicParsing
$stuDashExtHtml = $stuDashExtResp.Content

$hasApprovedBanner = $stuDashExtHtml.Contains("Your extension request for") -and $stuDashExtHtml.Contains("Notification Test Quiz Beta") -and $stuDashExtHtml.Contains("was approved")
Write-Host "Student1 Dashboard shows approved extension banner: $hasApprovedBanner"
if (-not $hasApprovedBanner) { throw "Student1 Dashboard did not show the approved extension update banner!" }

# student1 opens the Quizzes page (/quizzes) -> should mark it seen
$openQuizResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes" -WebSession $student1Session -UseBasicParsing
Write-Host "Student1 opened /quizzes page (status: $($openQuizResp.StatusCode))"

# Check DB: seen flag must now be 1
$seenFlag = (mysql -u root -proot -N -e "SELECT seen FROM rbac_db.quiz_extension_requests WHERE request_id=$reqId;").Trim()
Write-Host "DB seen flag after opening /quizzes: $seenFlag"
if ($seenFlag -ne "1") { throw "Extension request was not marked seen in DB!" }

# student1 returns to Dashboard -> update banner must be GONE (shown only once)
$stuDashFinalResp = Invoke-WebRequest -Uri "http://localhost:8080/dashboard" -WebSession $student1Session -UseBasicParsing
$stuDashFinalHtml = $stuDashFinalResp.Content

$bannerShownOnce = -not ($stuDashFinalHtml.Contains("Your extension request for") -and $stuDashFinalHtml.Contains("Notification Test Quiz Beta"))
Write-Host "Approved extension banner disappeared on subsequent view (shown once): $bannerShownOnce"
if (-not $bannerShownOnce) { throw "Extension update banner did not disappear on subsequent view!" }

Write-Host "`n===================================================="
Write-Host "ALL QUIZ NOTIFICATION TESTS PASSED SUCCESSFULLY!"
Write-Host "===================================================="
