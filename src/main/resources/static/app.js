const API_BASE = '/api/shifts';
let currentJobId = null;
let customFieldCount = 0;

document.addEventListener('DOMContentLoaded', () => {
    checkAuthStatus();

    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('register-form').addEventListener('submit', handleRegister);
    document.getElementById('logout-btn').addEventListener('click', handleLogout);
    document.getElementById('job-select').addEventListener('change', handleJobChange);
    document.getElementById('add-job-form').addEventListener('submit', handleAddJob);
    document.getElementById('add-custom-field-btn').addEventListener('click', addCustomFieldRow);
    document.getElementById('attribute-min-shifts-input').addEventListener('change', () => {
        if (currentJobId) loadAllData();
    });
    document.getElementById('min-shifts-input').addEventListener('change', () => {
        if (currentJobId) loadAllData();
    });
    document.getElementById('show-register-btn').addEventListener('click', () => {
        document.getElementById('login-form-container').style.display = 'none';
        document.getElementById('register-form-container').style.display = 'block';
    });
    document.getElementById('show-login-btn').addEventListener('click', () => {
        document.getElementById('register-form-container').style.display = 'none';
        document.getElementById('login-form-container').style.display = 'block';
    });

    document.querySelectorAll('.quick-range-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            selectQuickRange(parseInt(btn.dataset.months));
        });
    });

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

async function loadJobs(selectJobId = null) {
    const select = document.getElementById('job-select');

    try {
        const response = await fetch('/api/jobs');
        const jobs = await response.json();

        if (jobs.length === 0) {
            select.innerHTML = '<option value="">No jobs yet — add one below</option>';
            currentJobId = null;
            return;
        }

        select.innerHTML = jobs.map(job =>
            `<option value="${job.id}">${job.name}</option>`
        ).join('');

        const jobToSelect = selectJobId ?? jobs[0].id;
        select.value = jobToSelect;
        currentJobId = jobToSelect;

    } catch (err) {
        select.innerHTML = '<option value="">Failed to load jobs</option>';
    }
}

function handleJobChange(event) {
    currentJobId = parseInt(event.target.value);
    if (currentJobId) {
        loadAllData();
    }
}

async function handleAddJob(event) {
    event.preventDefault();

    const nameInput = document.getElementById('new-job-name');
    const name = nameInput.value;

    try {
        const response = await fetch('/api/jobs', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });

        if (response.ok) {
            const newJob = await response.json();
            nameInput.value = '';
            await loadJobs(newJob.id); // reload dropdown, auto-select the new job
            loadAllData();
        } else {
            alert('Failed to add job — name may already exist.');
        }
    } catch (err) {
        alert('Network error: ' + err.message);
    }
}

function getRangeMonthsAgo(months) {
    const end = new Date();
    const start = new Date();
    start.setMonth(start.getMonth() - months);

    const format = (d) => d.toISOString().split('T')[0]; // YYYY-MM-DD

    return { start: format(start), end: format(end) };
}

function selectQuickRange(months) {
    const { start, end } = getRangeMonthsAgo(months);
    document.getElementById('range-start').value = start;
    document.getElementById('range-end').value = end;

    setActiveQuickButton(months);
    loadAllData();
}

function setActiveQuickButton(months) {
    document.querySelectorAll('.quick-range-btn').forEach(btn => {
        const isActive = parseInt(btn.dataset.months) === months;
        btn.style.fontWeight = isActive ? 'bold' : 'normal';
    });
}

async function showApp(username) {
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('app-content').style.display = 'block';
    document.getElementById('current-username').textContent = username;

    await loadJobs();
    selectQuickRange(1);
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
        cardTips: parseFloat(document.getElementById('shift-card-tips').value),
        role: document.getElementById('shift-role').value,
        customAttributes: collectCustomAttributes()
    };

    const messageEl = document.getElementById('add-shift-message');

    try {
        const response = await fetch(`${API_BASE}?jobId=${currentJobId}`, {
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
        clearCustomFieldRows();

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

    if(!currentJobId) {
        return;
    }

    if (!start || !end) {
        alert('Please choose both a start and end date.');
        return;
    }

    await Promise.all([
        loadSummary(start, end),
        loadProfitability(start, end),
        loadAttributeProfitability(start, end),
        loadShiftsTable(start, end)
    ]);
}

async function loadSummary(start, end) {
    const output = document.getElementById('summary-output');
    try {
        const response = await fetch(`${API_BASE}/summary?jobId=${currentJobId}&start=${start}&end=${end}`);
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
    const minShifts = document.getElementById('min-shifts-input').value || 1;
    try {
        const response = await fetch(`${API_BASE}/profitability?jobId=${currentJobId}&start=${start}&end=${end}&minShifts=${minShifts}`);
        const data = await response.json();

        const renderEntries = (entries) =>
            entries.map(e =>
                `<li>${e.dayOfWeek} ${e.shiftType} (${e.role}) — Avg Gross: $${e.avgGross.toFixed(2)}, Avg Tips: $${e.avgTips.toFixed(2)} — based on ${e.count} shift${e.count === 1 ? '' : 's'}</li>`
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
        const response = await fetch(`${API_BASE}?jobId=${currentJobId}&start=${start}&end=${end}`);
        const shifts = await response.json();

        const customKeys = getUniqueCustomKeys(shifts);
        renderTableHeader(customKeys);

        tbody.innerHTML = shifts.map(shift => {
            const grossPay = (shift.hours * shift.wage) + shift.cashTips + shift.cardTips;

            const customCells = customKeys.map(key => {
                const value = shift.customAttributes?.[key];
                return `<td>${value ?? '—'}</td>`;
            }).join('');

            return `
                <tr>
                    <td>${shift.date}</td>
                    <td>${shift.type}</td>
                    <td>${shift.hours}</td>
                    <td>${shift.wage}</td>
                    <td>${shift.cashTips}</td>
                    <td>${shift.cardTips}</td>
                    <td>$${grossPay.toFixed(2)}</td>
                    ${customCells}
                    <td><button onclick="handleDeleteShift('${shift.date}', '${shift.type}')">Delete</button></td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8">Failed to load shifts: ${err.message}</td></tr>`;
    }
}

function getUniqueCustomKeys(shifts) {
    const keySet = new Set();

    shifts.forEach(shift => {
        if (shift.customAttributes) {
            Object.keys(shift.customAttributes).forEach(key => keySet.add(key));
        }
    });

    return Array.from(keySet).sort();
}

function renderTableHeader(customKeys) {
    const headerRow = document.getElementById('shifts-table-header-row');
    const baseHeaders = ['Date', 'Type', 'Hours', 'Wage', 'Cash Tips', 'Card Tips', 'Gross Pay'];

    const allHeadersHtml =
        baseHeaders.map(h => `<th>${h}</th>`).join('') +
        customKeys.map(key => `<th>${key}</th>`).join('') +
        `<th></th>`; // trailing column for the Delete button

    headerRow.innerHTML = allHeadersHtml;
}

async function handleDeleteShift(date, type) {
    if (!confirm(`Delete the ${type} shift on ${date}?`)) return;

    try {
        const response = await fetch(`${API_BASE}?jobId=${currentJobId}&?date=${date}&type=${type}`, {
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

async function loadAttributeProfitability(start, end) {
    const section = document.getElementById('attribute-profitability-section');
    const output = document.getElementById('attribute-profitability-output');
    const minShifts = document.getElementById('attribute-min-shifts-input').value || 1;

    try {
        const response = await fetch(`${API_BASE}/attribute-profitability?jobId=${currentJobId}&start=${start}&end=${end}&minShifts=${minShifts}`);
        const data = await response.json();

        if (!data || data.length === 0) {
            section.style.display = 'none';
            return;
        }

        section.style.display = 'block';
        output.innerHTML = data.map(group => `
            <h3>${group.key}</h3>
            <ul>
                ${group.values.map(v =>
                    `<li>${v.value} — Avg Gross: $${v.avgGross.toFixed(2)}, Avg Tips: $${v.avgTips.toFixed(2)} (${v.count} shift${v.count === 1 ? '' : 's'})</li>`
                ).join('')}
            </ul>
        `).join('');

    } catch (err) {
        section.style.display = 'block';
        output.textContent = 'Failed to load attribute profitability: ' + err.message;
    }
}

function addCustomFieldRow() {
    const container = document.getElementById('custom-attributes-list');
    const rowId = `custom-field-${customFieldCount++}`;

    const row = document.createElement('div');
    row.id = rowId;
    row.innerHTML = `
        <input type="text" placeholder="Field name (e.g. hair style)" class="custom-key">
        <input type="text" placeholder="Value (e.g. curled)" class="custom-value">
        <button type="button" onclick="document.getElementById('${rowId}').remove()">Remove</button>
    `;

    container.appendChild(row);
}

function collectCustomAttributes() {
    const attributes = {};
    const rows = document.querySelectorAll('#custom-attributes-list > div');

    rows.forEach(row => {
        const key = row.querySelector('.custom-key').value.trim();
        const value = row.querySelector('.custom-value').value.trim();
        if (key && value) {
            attributes[key] = value;
        }
    });

    return Object.keys(attributes).length > 0 ? attributes : null;
}

function clearCustomFieldRows() {
    document.getElementById('custom-attributes-list').innerHTML = '';
}