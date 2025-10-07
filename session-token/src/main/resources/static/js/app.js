// --------------------- API & AUTHENTICATION LOGIC (Unchanged) ---------------------

// Global variables to store tokens
let accessToken = null;
let refreshToken = null;

// API call utility function
async function apiCall(url, method, data) {
    try {
        const response = await fetch(`http://localhost:8080/api/auth${url}`, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                ...(accessToken && { 'Authorization': `Bearer ${accessToken}` })
            },
            body: JSON.stringify(data)
        });

        const responseText = await response.text();
        console.log('API Response:', responseText);

        try {
            return JSON.parse(responseText);
        } catch (parseError) {
            if (response.ok && (responseText.includes('created') || responseText.includes('success') || responseText.includes('Logged out'))) {
                return { success: true, message: responseText };
            } else {
                return { error: responseText };
            }
        }
    } catch (error) {
        console.error('API call error:', error);
        return { error: error.message };
    }
}

// Signup function
async function signup() {
    const username = document.getElementById('signupUser').value;
    const password = document.getElementById('signupPass').value;
    if (!username || !password) {
        showResult('signupResult', 'Please enter both username and password', 'error');
        return;
    }
    showLoading('signupResult');
    const result = await apiCall('/signup', 'POST', { username, password });
    if (result.success || result.message || !result.error) {
        showResult('signupResult', 'User created successfully!', 'success');
        document.getElementById('signupUser').value = '';
        document.getElementById('signupPass').value = '';
    } else {
        showResult('signupResult', `Error: ${result.error || result.message}`, 'error');
    }
}

// Login function
async function login() {
    const username = document.getElementById('loginUser').value;
    const password = document.getElementById('loginPass').value;
    if (!username || !password) {
        showResult('loginResult', 'Please enter both username and password', 'error');
        return;
    }
    showLoading('loginResult');
    const result = await apiCall('/login', 'POST', { username, password });
    if (result.accessToken) {
        accessToken = result.accessToken;
        refreshToken = result.refreshToken;
        updateSessionStatus();
        showResult('loginResult', 'Login successful!', 'success');
        document.getElementById('loginUser').value = '';
        document.getElementById('loginPass').value = '';
    } else {
        showResult('loginResult', `Login failed: ${result.error || 'Invalid credentials'}`, 'error');
    }
}

// Refresh tokens function
async function refreshTokens() {
    if (!refreshToken) {
        showResult('refreshResult', 'No refresh token available', 'error');
        return;
    }
    showLoading('refreshResult');
    const result = await apiCall('/refresh', 'POST', { refreshToken });
    if (result.accessToken) {
        accessToken = result.accessToken;
        refreshToken = result.refreshToken;
        updateSessionStatus();
        showResult('refreshResult', 'Tokens refreshed successfully!', 'success');
    } else {
        showResult('refreshResult', `Refresh failed: ${result.error}`, 'error');
    }
}

// Logout function
async function logout() {
    if (!refreshToken) {
        showResult('logoutResult', 'No active session', 'error');
        return;
    }
    showLoading('logoutResult');
    const result = await apiCall('/logout', 'POST', { refreshToken });
    accessToken = null;
    refreshToken = null;
    updateSessionStatus();
    if (result.success || result.message || !result.error) {
        showResult('logoutResult', 'Logged out successfully!', 'success');
    } else {
        showResult('logoutResult', `Logout failed: ${result.error}`, 'error');
    }
}

// --------------------- UI & DISPLAY LOGIC (Unchanged) ---------------------

// Update session status display
function updateSessionStatus() {
    const statusDiv = document.getElementById('sessionStatus');
    const tokensDiv = document.getElementById('tokens');
    if (accessToken && refreshToken) {
        statusDiv.innerHTML = '<div class="success">✅ Session Active - User is logged in</div>';
        tokensDiv.innerHTML = `
            <h3>Access Token:</h3><div class="token">${accessToken}</div>
            <h3>Refresh Token:</h3><div class="token">${refreshToken}</div>
            <p><small>Tokens are stored in memory and will be lost on page refresh</small></p>
        `;
    } else {
        statusDiv.innerHTML = '<div class="error">❌ No Active Session - Please log in</div>';
        tokensDiv.innerHTML = '';
    }
}

// Utility function to show results
function showResult(elementId, message, type) {
    const element = document.getElementById(elementId);
    element.innerHTML = `<div class="${type}">${message}</div>`;
}

// Utility function to show loading
function showLoading(elementId) {
    const element = document.getElementById(elementId);
    element.innerHTML = '<div class="loading"></div>';
}

// --------------------- CORRECTED INITIALIZATION LOGIC ---------------------

// Run all setup code after the page content is fully loaded
document.addEventListener('DOMContentLoaded', () => {

    // --- 1. SETUP TAB NAVIGATION (This is the fix) ---
    const tabs = document.querySelectorAll('.tab');
    const contentSections = document.querySelectorAll('.content-section');

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            // Remove 'active' class from all tabs and content sections
            tabs.forEach(item => item.classList.remove('active'));
            contentSections.forEach(item => item.classList.remove('active'));

            // Add 'active' class to the clicked tab and its corresponding content
            tab.classList.add('active');
            const targetId = tab.getAttribute('data-target');
            const targetSection = document.getElementById(targetId);
            if (targetSection) {
                targetSection.classList.add('active');
            }
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    });

    // --- 2. SETUP SIMULATION & VISUALS ---
    const accessBox = document.getElementById('accessTokenBox');
    const refreshBox = document.getElementById('refreshTokenBox');
    const simOutput = document.getElementById('simOutput');

    function syncVisualTokens() {
        if (accessToken && refreshToken) {
            accessBox.classList.remove('hidden', 'revoked');
            accessBox.classList.add('access');
            refreshBox.classList.remove('hidden', 'revoked');
            refreshBox.classList.add('refresh');
            simOutput.textContent = 'Session active — tokens received from backend.';
        } else {
            accessBox.classList.add('hidden');
            accessBox.classList.remove('access', 'revoked');
            refreshBox.classList.add('hidden');
            refreshBox.classList.remove('refresh', 'revoked');
            simOutput.textContent = 'No active session.';
        }
    }

    document.getElementById('btnSimLogin').addEventListener('click', async () => {
        const u = document.getElementById('loginUser');
        const p = document.getElementById('loginPass');
        if (u && p && !u.value && !p.value) {
            u.value = 'testuser';
            p.value = 'testpass';
        }
        await login();
        syncVisualTokens();
    });

    document.getElementById('btnSimRefresh').addEventListener('click', async () => {
        await refreshTokens();
        syncVisualTokens();
    });

    document.getElementById('btnSimLogout').addEventListener('click', async () => {
        await logout();
        accessBox.classList.add('revoked');
        accessBox.classList.remove('access');
        refreshBox.classList.add('revoked');
        refreshBox.classList.remove('refresh');
        setTimeout(syncVisualTokens, 450);
    });

    // Initial sync and periodic check for visual consistency
    syncVisualTokens();
    setInterval(syncVisualTokens, 700);

    // --- 3. ADD KEYPRESS LISTENERS FOR FORMS ---
    document.getElementById('signupPass').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') signup();
    });
    document.getElementById('loginPass').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') login();
    });

    // --- 4. INITIALIZE SESSION STATUS DISPLAY ON LOAD ---
    updateSessionStatus();
});

// --- Make functions globally accessible for the HTML onclick attributes ---
window.signup = signup;
window.login = login;
window.refreshTokens = refreshTokens;
window.logout = logout;