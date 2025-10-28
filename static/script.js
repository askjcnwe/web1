let selectedX = null;

function setX(value, button) {
    selectedX = value;
    document.querySelectorAll('#x-buttons button').forEach(b => b.classList.remove('active'));
    button.classList.add('active');
}

document.getElementById('point-form').addEventListener('submit', async e => {
    e.preventDefault();

    const yRaw = document.getElementById('y').value.trim();
    const y = parseFloat(yRaw.replace(',', '.'));
    const r = document.querySelector('input[name="r"]:checked')?.value;

    if (selectedX === null) {
        alert('Выберите X');
        return;
    }
    if (isNaN(y) || y < -3 || y > 3) {
        alert('Y должен быть числом в диапазоне [-3, 3]');
        return;
    }
    if (!r) {
        alert('Выберите R');
        return;
    }

    const params = new URLSearchParams({ x: selectedX, y, r });
    const requestTime = new Date().toLocaleTimeString();

    try {
        const response = await fetch(`/check?${params.toString()}`, { method: 'GET' });
        const data = await response.json();

        const tbody = document.querySelector('#results-table tbody');

        if (data.last) {
            const entry = data.last;
            const row = document.createElement('tr');

            row.innerHTML = `
                <td>${entry.x}</td>
                <td>${entry.y}</td>
                <td>${entry.r}</td>
                <td>${entry.result ? 'Попадание' : 'Мимо'}</td>
                <td>${entry.time_us}</td>
                <td>${requestTime}</td>
            `;
            tbody.prepend(row);
        }
    } catch (err) {
        console.error(err);
        alert('Ошибка соединения с сервером');
    }
});
