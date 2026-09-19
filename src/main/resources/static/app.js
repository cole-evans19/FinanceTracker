const API_BASE = '/api/shifts';

document.addEventListener('DOMContentLoaded', () => {
    checkAuthStatus();

    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('register-form').addEventListener('submit', handleRegister);
    document.getElementById('logout-btn').addEventListener('click', handleLogout);
    document.getElementById('show-register-btn').addEventListener('click', () => {
        document.getElementById('login-form-container').style.display = 'none';
        document.getElementById('register-form-container').style.display = 'block';
    });
    document.getElementById('show-login-btn').addEventListener('click', () => {
        document.getElementById('register-form-container').style.display = 'none';
        document.getElementById('login-form-container').style.display = 'block';
    });

    // your existing listeners stay here too:
    document.getElementById('add-shift-form').addEventListener('submit', handleAddShift);
    document.getElementById('load-data-btn').addEventListener('click', loadAllData);
});

async function checkAuthStatus() {
    try {
        const response = await fetch('/api/whoami');
        if (response.ok) {
            const data = await response.json();
            showApp(data.username);
        } else {
            showAuthForms();
        }
    } catch (err) {
        showAuthForms();
    }
}

function showApp(username) {
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('app-content').style.display = 'block';
    document.getElementById('current-username').textContent = username;
}

function showAuthForms() {
    document.getElementById('auth-section').style.display = 'block';
    document.getElementById('app-content').style.display = 'none';
}

async function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;
    const messageEl = document.getElementById('login-message');

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ username, password })
        });

        if (response.ok) {
            showApp(username);
        } else {
            messageEl.textContent = 'Invalid username or password.';
        }
    } catch (err) {
        messageEl.textContent = 'Network error: ' + err.message;
    }
}

async function handleRegister(event) {
    event.preventDefault();

    const username = document.getElementById('register-username').value;
    const password = document.getElementById('register-password').value;
    const messageEl = document.getElementById('register-message');

    try {
        const response = await fetch('/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const result = await response.json();

        if (response.ok) {
            messageEl.textContent = 'Registered! You can now log in.';
            document.getElementById('show-login-btn').click();
        } else {
            messageEl.textContent = 'Error: ' + result.message;
        }
    } catch (err) {
        messageEl.textContent = 'Network error: ' + err.message;
    }
}

async function handleLogout() {
    await fetch('/api/logout', { method: 'POST' });
    document.getElementById('login-form').reset();
    document.getElementById('register-form').reset();
    showAuthForms();
}

async function handleAddShift(event) {
    event.preventDefault();

    const shift = {
        date: document.getElementById('shift-date').value,
        type: document.getElementById('shift-type').value,
        hours: parseFloat(document.getElementById('shift-hours').value),
        wage: parseFloat(document.getElementById('shift-wage').value),
        cashTips: parseFloat(document.getElementById('shift-cash-tips').value),
        cardTips: parseFloat(document.getElementById('shift-card-tips').value)
    };

    const messageEl = document.getElementById('add-shift-message');

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(shift)
        });

        if (response.status === 409) {
            const error = await response.json();
            messageEl.textContent = 'Error: ' + error.message;
            return;
        }

        if (!response.ok) {
            messageEl.textContent = 'Something went wrong adding the shift.';
            return;
        }

        const result = await response.json();
        messageEl.textContent = result.message;
        document.getElementById('add-shift-form').reset();

        // Refresh the displayed data if a range is already loaded
        if (document.getElementById('range-start').value && document.getElementById('range-end').value) {
            loadAllData();
        }

    } catch (err) {
        messageEl.textContent = 'Network error: ' + err.message;
    }
}

async function loadAllData() {
    const start = document.getElementById('range-start').value;
    const end = document.getElementById('range-end').value;

    if (!start || !end) {
        alert('Please choose both a start and end date.');
        return;
    }

    await Promise.all([
        loadSummary(start, end),
        loadProfitability(start, end),
        loadShiftsTable(start, end)
    ]);
}

async function loadSummary(start, end) {
    const output = document.getElementById('summary-output');
    try {
        const response = await fetch(`${API_BASE}/summary?start=${start}&end=${end}`);
        const data = await response.json();

        output.innerHTML = `
            <p>Shifts worked: ${data.shiftsWorked}</p>
            <p>Gross pay: $${data.grossPay.toFixed(2)}</p>
            <p>Total cash tips: $${data.totalCashTips.toFixed(2)}</p>
            <p>Total card tips: $${data.totalCardTips.toFixed(2)}</p>
            <p>Avg shifts/week: ${data.avgShiftsPerWeek.toFixed(2)}</p>
            <p>Avg gross pay/week: $${data.avgGrossPayPerWeek.toFixed(2)}</p>
            <p>Avg tips/week: $${data.avgTipsPerWeek.toFixed(2)}</p>
        `;
    } catch (err) {
        output.textContent = 'Failed to load summary: ' + err.message;
    }
}

async function loadProfitability(start, end) {
    const output = document.getElementById('profitability-output');
    try {
        const response = await fetch(`${API_BASE}/profitability?start=${start}&end=${end}`);
        const data = await response.json();

        const renderEntries = (entries) =>
            entries.map(e =>
                `<li>${e.dayOfWeek} ${e.shiftType} — Avg Gross: $${e.avgGross.toFixed(2)}, Avg Tips: $${e.avgTips.toFixed(2)} (${e.count} shifts)</li>`
            ).join('');

        output.innerHTML = `
            <h3>Most Profitable</h3>
            <ul>${renderEntries(data.mostProfitable)}</ul>
            <h3>Least Profitable</h3>
            <ul>${renderEntries(data.leastProfitable)}</ul>
        `;
    } catch (err) {
        output.textContent = 'Failed to load profitability: ' + err.message;
    }
}

async function loadShiftsTable(start, end) {
    const tbody = document.getElementById('shifts-table-body');
    try {
        const response = await fetch(`${API_BASE}?start=${start}&end=${end}`);
        const shifts = await response.json();

        tbody.innerHTML = shifts.map(shift => {
            const grossPay = (shift.hours * shift.wage) + shift.cashTips + shift.cardTips;
            return `
                <tr>
                    <td>${shift.date}</td>
                    <td>${shift.type}</td>
                    <td>${shift.hours}</td>
                    <td>${shift.wage}</td>
                    <td>${shift.cashTips}</td>
                    <td>${shift.cardTips}</td>
                    <td>$${grossPay.toFixed(2)}</td>
                    <td><button onclick="handleDeleteShift('${shift.date}', '${shift.type}')">Delete</button></td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8">Failed to load shifts: ${err.message}</td></tr>`;
    }
}

async function handleDeleteShift(date, type) {
    if (!confirm(`Delete the ${type} shift on ${date}?`)) return;

    try {
        const response = await fetch(`${API_BASE}?date=${date}&type=${type}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadAllData(); // refresh everything after a delete
        } else {
            alert('Failed to delete shift.');
        }
    } catch (err) {
        alert('Network error: ' + err.message);
    }
}