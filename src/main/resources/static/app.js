const API_BASE = '/api/shifts';

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('add-shift-form').addEventListener('submit', handleAddShift);
    document.getElementById('load-data-btn').addEventListener('click', loadAllData);
});

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