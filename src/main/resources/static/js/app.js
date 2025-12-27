// Gemeinsame JavaScript-Funktionen

document.addEventListener('DOMContentLoaded', function() {
    // Event-Listener für Refresh-Button
    const refreshButton = document.querySelector('.refresh-button');
    if (refreshButton) {
        refreshButton.addEventListener('click', function() {
            window.location.reload();
        });
    }

    // Select All Checkbox-Funktionalität für Kontaktliste
    const selectAllCheckbox = document.getElementById('selectAll');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('.contact-checkbox');
            console.log('Toggle Select All:', this.checked, 'Found checkboxes:', checkboxes.length);
            checkboxes.forEach(cb => {
                cb.checked = this.checked;
            });
        });
    }
});
