/**
 * Progressive Enhancement: Animated Lock & Async Login Submit
 * Theme: Red (#d90429) & Black (#111 / #1a1a1a)
 */

document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('form[action*="/login"]') || document.querySelector('form');
    if (!form) return;

    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const submitBtn = form.querySelector('button[type="submit"]');
    const lockSvg = document.querySelector('.lock-svg');
    const lockCard = document.querySelector('.login-card');
    const lockMsg = document.getElementById('lockMsg');
    const capsWarning = document.getElementById('capsLockWarning');
    const togglePasswordBtn = document.getElementById('togglePasswordBtn');
    const togglePasswordIcon = document.getElementById('togglePasswordIcon');
    const serverErrorAlert = document.getElementById('serverErrorAlert');
    const serverLogoutAlert = document.getElementById('serverLogoutAlert');

    let failedAttempts = 0;
    let tiltDirection = 1;
    let tiltTimer = null;
    const isReducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // 1. Check Initial URL Query Parameters (?error / ?logout)
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        if (serverErrorAlert) serverErrorAlert.style.display = 'none';
        triggerFailureAnimation('Incorrect username or password. Please try again.');
    } else if (urlParams.has('logout')) {
        if (serverLogoutAlert) serverLogoutAlert.style.display = 'none';
        if (lockSvg && !isReducedMotion()) {
            lockSvg.classList.add('is-closing');
        }
        if (lockMsg) {
            lockMsg.textContent = "You've been logged out safely.";
            lockMsg.className = 'lock-msg';
            lockMsg.style.color = '#28a745';
        }
    }

    // 2. Typing Interactions: Tiny tilt & clear error border
    const handleTyping = () => {
        if (lockSvg && !isReducedMotion()) {
            lockSvg.style.transform = `rotate(${tiltDirection * 3.5}deg)`;
            tiltDirection = -tiltDirection;
            clearTimeout(tiltTimer);
            tiltTimer = setTimeout(() => {
                lockSvg.style.transform = '';
            }, 160);
        }
        if (usernameInput) usernameInput.classList.remove('is-invalid-lock');
        if (passwordInput) passwordInput.classList.remove('is-invalid-lock');
    };

    if (usernameInput) usernameInput.addEventListener('input', handleTyping);
    if (passwordInput) passwordInput.addEventListener('input', handleTyping);

    // 3. Caps Lock Warning Detector
    const checkCapsLock = (e) => {
        if (e.getModifierState && capsWarning) {
            if (e.getModifierState('CapsLock')) {
                capsWarning.classList.remove('d-none');
            } else {
                capsWarning.classList.add('d-none');
            }
        }
    };

    if (passwordInput) {
        passwordInput.addEventListener('keyup', checkCapsLock);
        passwordInput.addEventListener('keydown', checkCapsLock);
        passwordInput.addEventListener('focus', checkCapsLock);
        passwordInput.addEventListener('blur', () => {
            if (capsWarning) capsWarning.classList.add('d-none');
        });
    }

    // 4. Password Show / Hide Eye Toggle
    if (togglePasswordBtn && passwordInput) {
        togglePasswordBtn.addEventListener('click', function () {
            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                if (togglePasswordIcon) {
                    togglePasswordIcon.className = 'bi bi-eye-slash';
                }
                togglePasswordBtn.setAttribute('title', 'Hide password');
                togglePasswordBtn.setAttribute('aria-label', 'Hide password');
            } else {
                passwordInput.type = 'password';
                if (togglePasswordIcon) {
                    togglePasswordIcon.className = 'bi bi-eye';
                }
                togglePasswordBtn.setAttribute('title', 'Show password');
                togglePasswordBtn.setAttribute('aria-label', 'Show password');
            }
        });
    }

    // 5. Sparks Burst Generator
    function triggerSparks() {
        if (isReducedMotion()) return;
        const container = document.querySelector('.sparks-container');
        if (!container) return;
        container.innerHTML = '';
        const sparkCount = 14;
        for (let i = 0; i < sparkCount; i++) {
            const spark = document.createElement('span');
            spark.className = 'spark';
            const angle = (i / sparkCount) * 360 + (Math.random() * 20 - 10);
            const dist = 32 + Math.random() * 30;
            const rad = (angle * Math.PI) / 180;
            const tx = Math.cos(rad) * dist;
            const ty = Math.sin(rad) * dist;
            spark.style.setProperty('--tx', `${tx}px`);
            spark.style.setProperty('--ty', `${ty}px`);
            spark.style.animationDelay = `${Math.random() * 0.08}s`;
            container.appendChild(spark);
        }
        setTimeout(() => {
            container.innerHTML = '';
        }, 900);
    }

    // 6. Failure Animation & Messaging
    function triggerFailureAnimation(customMsg) {
        failedAttempts++;
        if (serverErrorAlert) serverErrorAlert.style.display = 'none';

        if (!isReducedMotion()) {
            if (lockSvg) {
                lockSvg.classList.remove('is-shaking', 'is-flashing');
                void lockSvg.offsetWidth; // Reflow
                lockSvg.classList.add('is-shaking', 'is-flashing');
                setTimeout(() => {
                    lockSvg.classList.remove('is-shaking', 'is-flashing');
                }, 520);
            }
            if (lockCard) {
                lockCard.classList.remove('is-shaking');
                void lockCard.offsetWidth; // Reflow
                lockCard.classList.add('is-shaking');
                setTimeout(() => {
                    lockCard.classList.remove('is-shaking');
                }, 500);
            }
        }

        if (usernameInput) usernameInput.classList.add('is-invalid-lock');
        if (passwordInput) {
            passwordInput.classList.add('is-invalid-lock');
            passwordInput.value = '';
            passwordInput.focus();
        }

        let message = customMsg || 'Incorrect username or password. Please try again.';
        if (failedAttempts >= 3) {
            message += ' Still stuck? Contact your administrator.';
        }

        if (lockMsg) {
            lockMsg.textContent = message;
            lockMsg.className = 'lock-msg msg-error';
        }
    }

    // 7. Success Animation
    function triggerSuccessAnimation(redirectUrl) {
        if (lockSvg) {
            lockSvg.classList.remove('is-verifying', 'is-shaking', 'is-flashing');
            lockSvg.classList.add('is-unlocked');
        }

        triggerSparks();

        const username = usernameInput ? usernameInput.value.trim() : 'User';
        if (lockMsg) {
            lockMsg.textContent = `Welcome, ${username}! Unlocking your dashboard...`;
            lockMsg.className = 'lock-msg msg-success';
        }

        setTimeout(() => {
            window.location.href = redirectUrl;
        }, 1300);
    }

    // 8. Intercept Form Submission (Progressive Enhancement)
    form.addEventListener('submit', async function (e) {
        e.preventDefault();

        const originalBtnHtml = submitBtn ? submitBtn.innerHTML : 'Log In';

        // Waiting State
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Verifying...';
        }

        if (lockSvg) {
            lockSvg.classList.remove('is-closing', 'is-shaking', 'is-flashing', 'is-unlocked');
            lockSvg.classList.add('is-verifying');
        }

        if (lockMsg) {
            lockMsg.textContent = 'Verifying credentials...';
            lockMsg.className = 'lock-msg';
            lockMsg.style.color = '#b5b5b5';
        }

        try {
            const formData = new FormData(form);
            const bodyParams = new URLSearchParams(formData);

            const response = await fetch(form.action || window.location.href, {
                method: 'POST',
                body: bodyParams,
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                credentials: 'same-origin',
                redirect: 'follow'
            });

            if (lockSvg) lockSvg.classList.remove('is-verifying');

            // Decide result based on final response URL:
            // Successful authentication redirects to /dashboard (res.url does not contain /login)
            // Failed authentication remains on /login or redirects to /login?error (res.url contains /login)
            const isFailed = response.url.includes('/login');

            if (isFailed) {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalBtnHtml;
                }
                triggerFailureAnimation();
            } else {
                triggerSuccessAnimation(response.url);
            }
        } catch (error) {
            // Network failure fallback: let browser perform normal form submit
            console.error('Fetch error during login, falling back to standard submit:', error);
            form.submit();
        }
    });
});
