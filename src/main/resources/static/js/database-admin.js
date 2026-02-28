// Database Admin Functionality

document.addEventListener('DOMContentLoaded', function() {
    const restoreModal = document.getElementById('restoreModal');
    const showRestoreModalBtn = document.getElementById('showRestoreModalBtn');
    const hideRestoreModalBtn = document.getElementById('hideRestoreModalBtn');
    const restoreForm = document.getElementById('restoreForm');
    const restoreFilenameInput = document.getElementById('restoreFilename');

    // Show restore modal
    if (showRestoreModalBtn) {
        showRestoreModalBtn.addEventListener('click', function() {
            restoreModal.style.display = 'flex';
        });
    }

    // Hide restore modal
    if (hideRestoreModalBtn) {
        hideRestoreModalBtn.addEventListener('click', function() {
            restoreModal.style.display = 'none';
        });
    }

    // Restore backup buttons
    const restoreButtons = document.querySelectorAll('.restore-backup-btn');
    restoreButtons.forEach(function(button) {
        button.addEventListener('click', function() {
            const filename = this.getAttribute('data-filename');
            if (confirm('Möchten Sie die Datenbank wirklich aus diesem Backup wiederherstellen?\n\nAlle aktuellen Daten werden überschrieben!\n\nBackup: ' + filename)) {
                restoreFilenameInput.value = filename;
                restoreForm.submit();
            }
        });
    });
});
