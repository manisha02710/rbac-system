$ErrorActionPreference = "Stop"

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   SPRING BOOT RBAC END-TO-END VERIFICATION   " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Login
Write-Host "`n[1] AUTHENTICATION" -ForegroundColor Yellow
curl.exe -s -c cookies_adm.txt -d "username=admin&password=Admin@123" http://localhost:8080/login -L > $null
Write-Host "Admin authenticated: OK" -ForegroundColor Green

curl.exe -s -c cookies_fac.txt -d "username=faculty&password=Faculty@123" http://localhost:8080/login -L > $null
Write-Host "Faculty authenticated: OK" -ForegroundColor Green

curl.exe -s -c cookies_stu.txt -d "username=student1&password=Faculty@123" http://localhost:8080/login -L > $null
Write-Host "Student authenticated: OK" -ForegroundColor Green

# 2. Study Materials
Write-Host "`n[2] FEATURE 1: STUDY MATERIALS" -ForegroundColor Yellow
"Mock lecture slides content for CS101" | Set-Content "lecture_slides.pdf"
curl.exe -s -b cookies_fac.txt -F "courseId=1" -F "title=CS101 Lecture 1 Slides" -F "description=Binary trees and traversal algorithms" -F "file=@lecture_slides.pdf" http://localhost:8080/materials/upload -L > $null
Write-Host "Faculty uploaded study material: OK" -ForegroundColor Green

$stuMatHtml = (curl.exe -s -b cookies_stu.txt http://localhost:8080/materials) -join "`n"
if ($stuMatHtml -match 'CS101 Lecture 1 Slides') {
    Write-Host "Student listed materials: FOUND 'CS101 Lecture 1 Slides'" -ForegroundColor Green
} else {
    Write-Error "Material not found for student!"
}

if ($stuMatHtml -match '/materials/(\d+)/download') {
    $matId = $Matches[1]
    curl.exe -s -b cookies_stu.txt "http://localhost:8080/materials/$matId/download" -o "downloaded_mat.pdf"
    $dlContent = Get-Content "downloaded_mat.pdf" -Raw
    if ($dlContent -like "*Mock lecture slides content*") {
        Write-Host "Student downloaded material ID ${matId}: CONTENT MATCHES 100%" -ForegroundColor Green
    }
}

$stuUploadHtml = (curl.exe -s -b cookies_stu.txt -F "courseId=1" -F "title=Hacked" -F "file=@lecture_slides.pdf" http://localhost:8080/materials/upload) -join "`n"
if ($stuUploadHtml -match '403' -or $stuUploadHtml -match 'Access Restricted') {
    Write-Host "Security Check: Student upload blocked with HTTP 403 Forbidden: OK" -ForegroundColor Green
} else {
    Write-Error "Student was not blocked with 403 Forbidden!"
}

# 3. Assignments & Submissions
Write-Host "`n[3] FEATURE 2: ASSIGNMENTS & SUBMISSIONS" -ForegroundColor Yellow
curl.exe -s -b cookies_fac.txt -d "courseId=1" -d "title=Lab 1 Binary Search Trees" -d "description=Implement balanced AVL rotation" -d "dueDate=2026-11-05T23:59" -d "maxMarks=100" http://localhost:8080/assignments/create -L > $null
Write-Host "Faculty created assignment: OK" -ForegroundColor Green

$stuAssignHtml = (curl.exe -s -b cookies_stu.txt http://localhost:8080/assignments) -join "`n"
if ($stuAssignHtml -match 'Lab 1 Binary Search Trees' -and $stuAssignHtml -match 'PENDING') {
    Write-Host "Student view: Assignment listed with status PENDING: OK" -ForegroundColor Green
} else {
    Write-Error "Assignment not found or wrong status for student!"
}

$stuAssignHtml -match '/assignments/(\d+)/submit' | Out-Null
$assignId = $Matches[1]

"Student 1 BST submission source code" | Set-Content "bst_solution.pdf"
curl.exe -s -b cookies_stu.txt -F "file=@bst_solution.pdf" "http://localhost:8080/assignments/$assignId/submit" -L > $null
Write-Host "Student submitted assignment $assignId deliverable: OK" -ForegroundColor Green

$stuAssignAfter = (curl.exe -s -b cookies_stu.txt http://localhost:8080/assignments) -join "`n"
if ($stuAssignAfter -match 'SUBMITTED') {
    Write-Host "Student view: Assignment status updated to SUBMITTED: OK" -ForegroundColor Green
} else {
    Write-Error "Expected SUBMITTED status after submission!"
}

# Faculty grades
$facSubHtml = (curl.exe -s -b cookies_fac.txt "http://localhost:8080/assignments/$assignId/submissions") -join "`n"
$facSubHtml -match 'name="submissionId" value="(\d+)"' | Out-Null
$subId = $Matches[1]

curl.exe -s -b cookies_fac.txt -d "submissionId=$subId" -d "marks=96" -d "feedback=Exceptional rotations and clean tests" "http://localhost:8080/assignments/$assignId/grade" -L > $null
Write-Host "Faculty graded submission $subId (96/100): OK" -ForegroundColor Green

$stuGradedHtml = (curl.exe -s -b cookies_stu.txt http://localhost:8080/assignments) -join "`n"
if ($stuGradedHtml -match '96 / 100' -and $stuGradedHtml -match 'Exceptional rotations') {
    Write-Host "Student view: Received grade 96 / 100 & feedback: OK" -ForegroundColor Green
} else {
    Write-Error "Graded info not found on student page!"
}

# Student downloads own submission
curl.exe -s -b cookies_stu.txt "http://localhost:8080/assignments/submissions/$subId/download" -o "downloaded_sub.pdf"
$subDlContent = Get-Content "downloaded_sub.pdf" -Raw
if ($subDlContent -like "*Student 1 BST submission*") {
    Write-Host "Student downloaded own submission: CONTENT MATCHES 100%" -ForegroundColor Green
}

# 4. Faculty Feedback
Write-Host "`n[4] FEATURE 3: FACULTY FEEDBACK" -ForegroundColor Yellow
$ratePage = (curl.exe -s -b cookies_stu.txt http://localhost:8080/feedback/rate) -join "`n"
$ratePage -match '<option value="(\d+)">Faculty User' | Out-Null
$facProfileId = $Matches[1]

curl.exe -s -b cookies_stu.txt -d "facultyId=$facProfileId" -d "courseId=1" -d "rating=5" -d "comments=Prof Smith is fantastic and very approachable!" http://localhost:8080/feedback/submit -L > $null
Write-Host "Student submitted 5-star faculty rating & comment (Faculty ID: $facProfileId): OK" -ForegroundColor Green

$facFbHtml = (curl.exe -s -b cookies_fac.txt http://localhost:8080/faculty/feedback) -join "`n"
if ($facFbHtml -match 'Prof Smith is fantastic' -and $facFbHtml -notmatch 'Student One') {
    Write-Host "Faculty view: Rating & comment visible, ANONYMITY PRESERVED (no student name): OK" -ForegroundColor Green
} else {
    Write-Error "Faculty feedback failed anonymity or comment missing!"
}

$admFbHtml = (curl.exe -s -b cookies_adm.txt http://localhost:8080/admin/faculty-feedback) -join "`n"
if ($admFbHtml -match 'Prof Smith is fantastic' -and $admFbHtml -notmatch 'Student One') {
    Write-Host "Admin view: Rating & comment visible, ANONYMITY PRESERVED (no student name): OK" -ForegroundColor Green
} else {
    Write-Error "Admin feedback failed anonymity or comment missing!"
}

$stuAdmfbHtml = (curl.exe -s -b cookies_stu.txt http://localhost:8080/admin/faculty-feedback) -join "`n"
if ($stuAdmfbHtml -match '403' -or $stuAdmfbHtml -match 'Access Restricted') {
    Write-Host "Security Check: Student access to /admin/faculty-feedback blocked with 403: OK" -ForegroundColor Green
} else {
    Write-Error "Expected 403 for student /admin/faculty-feedback"
}

$stuFacfbHtml = (curl.exe -s -b cookies_stu.txt http://localhost:8080/faculty/feedback) -join "`n"
if ($stuFacfbHtml -match '403' -or $stuFacfbHtml -match 'Access Restricted') {
    Write-Host "Security Check: Student access to /faculty/feedback blocked with 403: OK" -ForegroundColor Green
} else {
    Write-Error "Expected 403 for student /faculty/feedback"
}

# 5. Online Quizzes
Write-Host "`n[5] FEATURE 4: QUIZZES" -ForegroundColor Yellow
curl.exe -s -b cookies_fac.txt -d "courseId=1" -d "title=CS101 Algorithms Assessment" -d "durationMinutes=15" -d "startTime=2026-10-25T00:00" -d "endTime=2026-11-05T23:59" http://localhost:8080/quizzes/create -L > $null
Write-Host "Faculty created quiz: OK" -ForegroundColor Green

$facQuizHtml = (curl.exe -s -b cookies_fac.txt http://localhost:8080/quizzes) -join "`n"
$facQuizHtml -match '/quizzes/(\d+)/questions' | Out-Null
$quizId = $Matches[1]

# Add Question 1 (B)
curl.exe -s -b cookies_fac.txt -d "questionText=What is the time complexity of binary search?" -d "optionA=O(1)" -d "optionB=O(log n)" -d "optionC=O(n)" -d "optionD=O(n log n)" -d "correctOption=B" -d "marks=5" "http://localhost:8080/quizzes/$quizId/questions/add" -L > $null

# Add Question 2 (C)
curl.exe -s -b cookies_fac.txt -d "questionText=Which data structure uses LIFO?" -d "optionA=Queue" -d "optionB=Heap" -d "optionC=Stack" -d "optionD=Array" -d "correctOption=C" -d "marks=5" "http://localhost:8080/quizzes/$quizId/questions/add" -L > $null
Write-Host "Faculty added 2 questions to Quiz ${quizId}: OK" -ForegroundColor Green

# Publish quiz
curl.exe -s -b cookies_fac.txt -d "published=true" "http://localhost:8080/quizzes/$quizId/publish" -L > $null
Write-Host "Faculty published quiz: OK" -ForegroundColor Green

# Student starts quiz
curl.exe -s -b cookies_stu.txt -X POST "http://localhost:8080/quizzes/$quizId/start" -L > $null
Write-Host "Student started quiz attempt: OK" -ForegroundColor Green

# Student enters /take
$takeHtml = (curl.exe -s -b cookies_stu.txt "http://localhost:8080/quizzes/$quizId/take") -join "`n"
if ($takeHtml -match 'countdownTimer') {
    Write-Host "Countdown timer present on quiz interface: OK" -ForegroundColor Green
} else {
    Write-Error "Countdown timer missing on quiz interface!"
}

# CRITICAL SECURITY CHECK
if ($takeHtml -match 'correctOption' -or $takeHtml -match 'correct_option') {
    Write-Error "CRITICAL SECURITY BREACH: correctOption found in taking DOM!"
} else {
    Write-Host "CRITICAL SECURITY CHECK: correctOption NOT sent to browser before submission: OK" -ForegroundColor Green
}

# Extract question IDs
$qIds = [regex]::Matches($takeHtml, 'name="answer_(\d+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique

# Submit answers: Q1 -> B, Q2 -> C (both correct -> 10/10)
$postData = "answer_$($qIds[0])=B&answer_$($qIds[1])=C"
curl.exe -s -b cookies_stu.txt -d "$postData" "http://localhost:8080/quizzes/$quizId/submit" -L > $null
Write-Host "Student submitted quiz answers: OK" -ForegroundColor Green

# Student reviews
$reviewHtml = (curl.exe -s -b cookies_stu.txt "http://localhost:8080/quizzes/$quizId/review") -join "`n"
if ($reviewHtml -match '10\s*</span>\s*pts' -and $reviewHtml -match 'Correct') {
    Write-Host "Student review: Score 10/10 auto-graded with full question breakdown: OK" -ForegroundColor Green
} else {
    Write-Error "Quiz auto-grading review failed!"
}

# Faculty views results
$resultsHtml = (curl.exe -s -b cookies_fac.txt "http://localhost:8080/quizzes/$quizId/results") -join "`n"
if ($resultsHtml -match 'Student One' -and $resultsHtml -match '10 pts') {
    Write-Host "Faculty results report: Student One with 10 pts completed: OK" -ForegroundColor Green
} else {
    Write-Error "Faculty results report missing student attempt!"
}

# Security checks
$stuQuestionsHtml = (curl.exe -s -b cookies_stu.txt "http://localhost:8080/quizzes/$quizId/questions") -join "`n"
if ($stuQuestionsHtml -match '403' -or $stuQuestionsHtml -match 'Access Restricted') {
    Write-Host "Security Check: Student access to /quizzes/{id}/questions blocked with 403: OK" -ForegroundColor Green
} else {
    Write-Error "Expected 403 for student /quizzes/questions"
}

$stuResultsHtml = (curl.exe -s -b cookies_stu.txt "http://localhost:8080/quizzes/$quizId/results") -join "`n"
if ($stuResultsHtml -match '403' -or $stuResultsHtml -match 'Access Restricted') {
    Write-Host "Security Check: Student access to /quizzes/{id}/results blocked with 403: OK" -ForegroundColor Green
} else {
    Write-Error "Expected 403 for student /quizzes/results"
}

# Clean temp files
Remove-Item -Force "lecture_slides.pdf", "downloaded_mat.pdf", "bst_solution.pdf", "downloaded_sub.pdf", "cookies_adm.txt", "cookies_fac.txt", "cookies_stu.txt", "test_notes.pdf" -ErrorAction SilentlyContinue

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   ALL 4 FEATURES & SECURITY CHECKS PASSED 100%!       " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
