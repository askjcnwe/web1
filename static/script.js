let selectedX = null;

function setX(value, button) {
    selectedX = value;
    document.querySelectorAll('#x-buttons button').forEach(b => b.classList.remove('active'));
    button.classList.add('active');
}

document.getElementById('point-form').addEventListener('submit', async e => {
    e.preventDefault(); // AJAX GET

    const yRaw = document.getElementById('y').value.trim();
    const y = parseFloat(yRaw.replace(',', '.'));
    const r = document.querySelector('input[name="r"]:checked')?.value;

    // Валидация
    if (selectedX === null) {
        alert('Выберите X');
        return;
    }
    if (isNaN(y) || y < -3 || y > 3) {
        alert('Y должен быть числом в диапазоне [-3, 3]');
        return;
    }
    if (!r || isNaN(parseInt(r)) || parseInt(r) < 1 || parseInt(r) > 5) {
        alert('Выберите корректный R');
        return;
    }

    const params = new URLSearchParams({ x: selectedX, y: y, r: r });
    try {
        const response = await fetch(`/check?${params.toString()}`, { method: 'GET' });
        if (!response.ok) throw new Error('HTTP error ' + response.status);

        const text = await response.text();
        let data;
        try {
            data = JSON.parse(text);
        } catch {
            alert('Сервер вернул некорректный ответ');
            return;
        }

        const tbody = document.querySelector('#results-table tbody');
        tbody.innerHTML = '';

        if (data.history && Array.isArray(data.history)) {
            data.history.forEach(entry => {
                const row = document.createElement('tr');
                const xv = entry.x ?? '';
                const yv = entry.y ?? '';
                const rv = entry.r ?? '';
                const res = entry.result ? 'Попадание' : 'Мимо';
                const timeUs = entry.time_us ?? '';
                const localTime = new Date().toLocaleTimeString();

                row.innerHTML = `
                    <td>${xv}</td>
                    <td>${yv}</td>
                    <td>${rv}</td>
                    <td>${res}</td>
                    <td>${timeUs}</td>
                    <td>${localTime}</td>`;
                tbody.appendChild(row);
            });
        } else if (data.last) {
            const entry = data.last;
            const row = document.createElement('tr');
            row.innerHTML = `<td>${entry.x||''}</td><td>${entry.y||''}</td><td>${entry.r||''}</td><td>${entry.result||entry.error||''}</td><td>${entry.time_us||''}</td><td>${new Date().toLocaleTimeString()}</td>`;
            tbody.appendChild(row);
        }
    } catch (error) {
        alert('Ошибка при соединении с сервером');
        console.error(error);
    }
});
