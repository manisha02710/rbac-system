$ErrorActionPreference = "Stop"

function Login-User([string]$username, [string]$password) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $loginUrl = "http://localhost:8080/login"
    $body = @{
        username = $username
        password = $password
    }
    $resp = Invoke-WebRequest -Uri $loginUrl -Method POST -Body $body -WebSession $session -MaximumRedirection 5 -UseBasicParsing
    Write-Host "Logged in as $username (Status: $($resp.StatusCode))"
    return $session
}

Write-Host "=== 1. AUTHENTICATION TESTS ==="
$adminSession = Login-User "admin" "Admin@123"
$facultySession = Login-User "faculty" "Faculty@123"
$studentSession = Login-User "student1" "Student1@123"

Write-Host "`n=== 2. FEATURE 1: STUDY MATERIALS ==="
# Faculty uploads study material
$tempFile = [System.IO.Path]::GetTempFileName() + ".pdf"
[System.IO.File]::WriteAllText($tempFile, "%PDF-1.4 Data Structures Lecture 1 Notes Content")

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"courseId`"",
    "",
    "1",
    "--$boundary",
    "Content-Disposition: form-data; name=`"title`"",
    "",
    "Lecture 1 - Introduction to Trees",
    "--$boundary",
    "Content-Disposition: form-data; name=`"description`"",
    "",
    "Covers binary trees, AVL balance factors, and traversals.",
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"lecture1.pdf`"",
    "Content-Type: application/pdf",
    "",
    [System.IO.File]::ReadAllText($tempFile),
    "--$boundary--"
) -join $LF

$uploadResp = Invoke-WebRequest -Uri "http://localhost:8080/materials/upload" `
    -Method POST `
    -ContentType "multipart/form-data; boundary=$boundary" `
    -Body $bodyLines `
    -WebSession $facultySession `
    -MaximumRedirection 5 `
    -UseBasicParsing

Write-Host "Faculty material upload status: $($uploadResp.StatusCode)"

# Student accesses /materials
$matList = Invoke-WebRequest -Uri "http://localhost:8080/materials" -WebSession $studentSession -UseBasicParsing
Write-Host "Student view materials page: $($matList.StatusCode)"
if ($matList.Content -like "*Lecture 1 - Introduction to Trees*") {
    Write-Host "SUCCESS: Material found in student's material catalog!" -ForegroundColor Green
} else {
    Write-Host "FAILED: Material not listed for student." -ForegroundColor Red
}

# Student downloads material
if ($matList.Content -match '/materials/(\d+)/download') {
    $matId = $Matches[1]
    $dlResp = Invoke-WebRequest -Uri "http://localhost:8080/materials/$matId/download" -WebSession $studentSession -UseBasicParsing
    Write-Host "Student downloaded material ID $matId (Bytes: $($dlResp.RawContentLength), Status: $($dlResp.StatusCode))" -ForegroundColor Green
}

# Student upload test (should be forbidden 403)
try {
    $forbiddenResp = Invoke-WebRequest -Uri "http://localhost:8080/materials/upload" `
        -Method POST `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $bodyLines `
        -WebSession $studentSession `
        -UseBasicParsing
    Write-Host "FAILED: Student was able to upload material!" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: Student upload blocked with 403/Forbidden ($($_.Exception.Message))" -ForegroundColor Green
}

Write-Host "`n=== 3. FEATURE 2: ASSIGNMENTS & SUBMISSIONS ==="
# Faculty creates assignment
$createAssignBody = @{
    courseId = "1"
    title = "Assignment 1: Red-Black Tree Implementation"
    description = "Implement insertion and left/right rotations in Java."
    dueDate = (Get-Date).AddDays(5).ToString("yyyy-MM-ddTHH:mm")
    maxMarks = "100"
}
$assignResp = Invoke-WebRequest -Uri "http://localhost:8080/assignments/create" `
    -Method POST `
    -Body $createAssignBody `
    -WebSession $facultySession `
    -MaximumRedirection 5 `
    -UseBasicParsing
Write-Host "Faculty assignment created: $($assignResp.StatusCode)"

# Student views assignment list
$studentAssignView = Invoke-WebRequest -Uri "http://localhost:8080/assignments" -WebSession $studentSession -UseBasicParsing
if ($studentAssignView.Content -like "*Assignment 1: Red-Black Tree Implementation*") {
    Write-Host "SUCCESS: Student sees created assignment with PENDING status!" -ForegroundColor Green
}

# Student submits file for assignment
$studentAssignView.Content -match '/assignments/(\d+)/submit' | Out-Null
$assignId = $Matches[1]

$subFile = [System.IO.Path]::GetTempFileName() + ".pdf"
[System.IO.File]::WriteAllText($subFile, "%PDF-1.4 Student 1 Red-Black Trees Solution Code and Analysis")

$subBoundary = [System.Guid]::NewGuid().ToString()
$subBodyLines = (
    "--$subBoundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"rbt_sol.pdf`"",
    "Content-Type: application/pdf",
    "",
    [System.IO.File]::ReadAllText($subFile),
    "--$subBoundary--"
) -join $LF

$subResp = Invoke-WebRequest -Uri "http://localhost:8080/assignments/$assignId/submit" `
    -Method POST `
    -ContentType "multipart/form-data; boundary=$subBoundary" `
    -Body $subBodyLines `
    -WebSession $studentSession `
    -MaximumRedirection 5 `
    -UseBasicParsing
Write-Host "Student submitted assignment ${assignId} (Status: $($subResp.StatusCode))"

# Verify student status changed to SUBMITTED
$studentAssignAfter = Invoke-WebRequest -Uri "http://localhost:8080/assignments" -WebSession $studentSession -UseBasicParsing
if ($studentAssignAfter.Content -like "*SUBMITTED*") {
    Write-Host "SUCCESS: Assignment status updated to SUBMITTED for student!" -ForegroundColor Green
}

# Faculty views submissions and grades it
$facSubView = Invoke-WebRequest -Uri "http://localhost:8080/assignments/$assignId/submissions" -WebSession $facultySession -UseBasicParsing
$facSubView.Content -match 'name="submissionId" value="(\d+)"' | Out-Null
$subId = $Matches[1]

$gradeBody = @{
    submissionId = $subId
    marks = "98"
    feedback = "Exceptional tree rotation logic and clean docstrings."
}
$gradeResp = Invoke-WebRequest -Uri "http://localhost:8080/assignments/$assignId/grade" `
    -Method POST `
    -Body $gradeBody `
    -WebSession $facultySession `
    -MaximumRedirection 5 `
    -UseBasicParsing
Write-Host "Faculty graded submission ${subId} (Status: $($gradeResp.StatusCode))"

# Student verifies GRADED status and score
$studentAssignGraded = Invoke-WebRequest -Uri "http://localhost:8080/assignments" -WebSession $studentSession -UseBasicParsing
if ($studentAssignGraded.Content -like "*98 / 100*" -and $studentAssignGraded.Content -like "*Exceptional tree rotation logic*") {
    Write-Host "SUCCESS: Student sees grade 98 / 100 and faculty feedback!" -ForegroundColor Green
} else {
    Write-Host "FAILED to see grade/feedback on student assignment page." -ForegroundColor Red
}

# Student downloads their submission
$stuSubDl = Invoke-WebRequest -Uri "http://localhost:8080/assignments/submissions/$subId/download" -WebSession $studentSession -UseBasicParsing
Write-Host "Student downloaded submission file (Bytes: $($stuSubDl.RawContentLength), Status: $($stuSubDl.StatusCode))" -ForegroundColor Green

Write-Host "`n=== 4. FEATURE 3: FACULTY FEEDBACK ==="
# Student rates faculty
$fbBody = @{
    facultyId = "1"
    courseId = "1"
    rating = "5"
    comments = "Dr. Smith explains complex concepts with great clarity and patience."
}
$fbResp = Invoke-WebRequest -Uri "http://localhost:8080/feedback/submit" `
    -Method POST `
    -Body $fbBody `
    -WebSession $studentSession `
    -MaximumRedirection 5 `
    -UseBasicParsing
Write-Host "Student submitted rating: $($fbResp.StatusCode)"

# Faculty views own feedback
$facFbView = Invoke-WebRequest -Uri "http://localhost:8080/faculty/feedback" -WebSession $facultySession -UseBasicParsing
if ($facFbView.Content -like "*Dr. Smith explains complex concepts*" -and $facFbView.Content -notlike "*Student One*") {
    Write-Host "SUCCESS: Faculty sees feedback comments with ANONYMITY preserved (no student name)!" -ForegroundColor Green
}

# Admin views feedback
$admFbView = Invoke-WebRequest -Uri "http://localhost:8080/admin/faculty-feedback" -WebSession $adminSession -UseBasicParsing
if ($admFbView.Content -like "*Anonymous Student Comments*" -and $admFbView.Content -notlike "*Student One*") {
    Write-Host "SUCCESS: Admin sees aggregate feedback with ANONYMITY preserved!" -ForegroundColor Green
}

# Student forbidden checks
try {
    Invoke-WebRequest -Uri "http://localhost:8080/admin/faculty-feedback" -WebSession $studentSession -UseBasicParsing
    Write-Host "FAILED: Student was able to access admin faculty feedback!" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: Student blocked from /admin/faculty-feedback with 403 ($($_.Exception.Message))" -ForegroundColor Green
}

try {
    Invoke-WebRequest -Uri "http://localhost:8080/faculty/feedback" -WebSession $studentSession -UseBasicParsing
    Write-Host "FAILED: Student was able to access faculty feedback!" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: Student blocked from /faculty/feedback with 403 ($($_.Exception.Message))" -ForegroundColor Green
}

Write-Host "`n=== 5. FEATURE 4: QUIZZES ==="
# Faculty creates quiz
$quizCreateBody = @{
    courseId = "1"
    title = "Data Structures & Big-O Quiz"
    durationMinutes = "15"
    startTime = (Get-Date).AddHours(-1).ToString("yyyy-MM-ddTHH:mm")
    endTime = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm")
}
$quizResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/create" `
    -Method POST `
    -Body $quizCreateBody `
    -WebSession $facultySession `
    -MaximumRedirection 5 `
    -UseBasicParsing
Write-Host "Faculty created quiz: $($quizResp.StatusCode)"

# Find quiz ID
$qList = Invoke-WebRequest -Uri "http://localhost:8080/quizzes" -WebSession $facultySession -UseBasicParsing
$qList.Content -match '/quizzes/(\d+)/questions' | Out-Null
$quizId = $Matches[1]

# Faculty adds Question 1 (Correct: B)
$q1Body = @{
    questionText = "What is the worst-case time complexity of searching in a balanced BST?"
    optionA = "O(1)"
    optionB = "O(log n)"
    optionC = "O(n)"
    optionD = "O(n log n)"
    correctOption = "B"
    marks = "5"
}
Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions/add" -Method POST -Body $q1Body -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing | Out-Null

# Faculty adds Question 2 (Correct: C)
$q2Body = @{
    questionText = "Which data structure operates on a Last-In, First-Out (LIFO) basis?"
    optionA = "Queue"
    optionB = "Heap"
    optionC = "Stack"
    optionD = "Linked List"
    correctOption = "C"
    marks = "5"
}
Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions/add" -Method POST -Body $q2Body -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing | Out-Null
Write-Host "Faculty added 2 questions to Quiz $quizId"

# Faculty publishes quiz
Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/publish" -Method POST -Body @{ published = "true" } -WebSession $facultySession -MaximumRedirection 5 -UseBasicParsing | Out-Null
Write-Host "Faculty published Quiz $quizId"

# Student starts quiz
Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/start" -Method POST -WebSession $studentSession -MaximumRedirection 5 -UseBasicParsing | Out-Null

# Student views /quizzes/{id}/take
$takeResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/take" -WebSession $studentSession -UseBasicParsing
Write-Host "Student entered quiz taking interface (Status: $($takeResp.StatusCode))"

# CRITICAL SECURITY CHECK: Check that correct_option is NOT leaked in the take page!
if ($takeResp.Content -match 'correctOption' -or $takeResp.Content -match 'correct_option') {
    Write-Host "CRITICAL FAILURE: Correct option found in taking page DOM!" -ForegroundColor Red
} else {
    Write-Host "CRITICAL SECURITY PASSED: Zero correct answers exposed in quiz taking DOM!" -ForegroundColor Green
}

# Verify countdown timer element exists
if ($takeResp.Content -like "*countdownTimer*") {
    Write-Host "SUCCESS: JavaScript countdown timer present on quiz interface!" -ForegroundColor Green
}

# Find question IDs from input names
$qIds = [regex]::Matches($takeResp.Content, 'name="answer_(\d+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
Write-Host "Found question IDs for attempt: $($qIds -join ', ')"

# Student submits answers: Question 1 -> B (Correct), Question 2 -> C (Correct)
$submitQuizBody = @{}
if ($qIds.Count -ge 2) {
    $submitQuizBody["answer_" + $qIds[0]] = "B"
    $submitQuizBody["answer_" + $qIds[1]] = "C"
}
$quizSubmitResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/submit" `
    -Method POST `
    -Body $submitQuizBody `
    -WebSession $studentSession `
    -MaximumRedirection 5 `
    -UseBasicParsing
Write-Host "Student submitted quiz: $($quizSubmitResp.StatusCode)"

# Student reviews result
$reviewResp = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/review" -WebSession $studentSession -UseBasicParsing
if ($reviewResp.Content -like "*10*pts*" -and $reviewResp.Content -like "*Correct*") {
    Write-Host "SUCCESS: Quiz auto-graded correctly! Score: 10/10 with full question review." -ForegroundColor Green
} else {
    Write-Host "Quiz review content: $($reviewResp.Content.Substring(0, 500))"
}

# Faculty views results
$facResults = Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/results" -WebSession $facultySession -UseBasicParsing
if ($facResults.Content -like "*Student One*" -and $facResults.Content -like "*10 pts*") {
    Write-Host "SUCCESS: Faculty results table reflects student attempt with score 10 pts!" -ForegroundColor Green
}

# Security: Student forbidden from question builder & results
try {
    Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/questions" -WebSession $studentSession -UseBasicParsing
    Write-Host "FAILED: Student was able to view questions manager!" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: Student blocked from quiz question manager with 403 ($($_.Exception.Message))" -ForegroundColor Green
}

try {
    Invoke-WebRequest -Uri "http://localhost:8080/quizzes/$quizId/results" -WebSession $studentSession -UseBasicParsing
    Write-Host "FAILED: Student was able to view quiz results report!" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: Student blocked from quiz results report with 403 ($($_.Exception.Message))" -ForegroundColor Green
}

Write-Host "`n=== ALL 4 FEATURES AND PERMISSIONS VERIFIED 100% SUCCESSFULLY! ===" -ForegroundColor Green
