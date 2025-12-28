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

    // Hashtag-Zuweisung Modal-Funktionalität
    const assignHashtagBtn = document.getElementById('assignHashtagBtn');
    const hashtagModal = document.getElementById('hashtagModal');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const cancelModalBtn = document.getElementById('cancelModalBtn');
    const assignHashtagForm = document.getElementById('assignHashtagForm');

    if (assignHashtagBtn && hashtagModal) {
        // Open Modal
        assignHashtagBtn.addEventListener('click', function() {
            const checkedBoxes = document.querySelectorAll('.contact-checkbox:checked');

            if (checkedBoxes.length === 0) {
                alert('Bitte wählen Sie mindestens einen Kontakt aus.');
                return;
            }

            // Clear previous hidden inputs
            const container = document.getElementById('selectedContactIdsContainer');
            container.innerHTML = '';

            // Add hidden inputs for selected contact IDs
            checkedBoxes.forEach(cb => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'contactIds';
                input.value = cb.value;
                container.appendChild(input);
            });

            // Update info text
            const infoText = document.getElementById('selectedContactsInfo');
            infoText.textContent = `${checkedBoxes.length} Kontakt(e) ausgewählt`;

            // Show modal
            hashtagModal.style.display = 'flex';
        });

        // Close Modal
        const closeModal = function() {
            hashtagModal.style.display = 'none';
            document.getElementById('hashtagSelect').value = '';
        };

        if (closeModalBtn) {
            closeModalBtn.addEventListener('click', closeModal);
        }

        if (cancelModalBtn) {
            cancelModalBtn.addEventListener('click', closeModal);
        }

        // Close modal when clicking outside
        hashtagModal.addEventListener('click', function(e) {
            if (e.target === hashtagModal) {
                closeModal();
            }
        });
    }

    // Hashtag-Entfernung Modal-Funktionalität
    const removeHashtagBtn = document.getElementById('removeHashtagBtn');
    const removeHashtagModal = document.getElementById('removeHashtagModal');
    const closeRemoveModalBtn = document.getElementById('closeRemoveModalBtn');
    const cancelRemoveModalBtn = document.getElementById('cancelRemoveModalBtn');
    const removeHashtagForm = document.getElementById('removeHashtagForm');

    if (removeHashtagBtn && removeHashtagModal) {
        // Open Remove Modal
        removeHashtagBtn.addEventListener('click', function() {
            const checkedBoxes = document.querySelectorAll('.contact-checkbox:checked');

            if (checkedBoxes.length === 0) {
                alert('Bitte wählen Sie mindestens einen Kontakt aus.');
                return;
            }

            // Clear previous hidden inputs
            const container = document.getElementById('selectedContactIdsContainerRemove');
            container.innerHTML = '';

            // Add hidden inputs for selected contact IDs
            checkedBoxes.forEach(cb => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'contactIds';
                input.value = cb.value;
                container.appendChild(input);
            });

            // Update info text
            const infoText = document.getElementById('selectedContactsInfoRemove');
            infoText.textContent = `${checkedBoxes.length} Kontakt(e) ausgewählt`;

            // Show modal
            removeHashtagModal.style.display = 'flex';
        });

        // Close Remove Modal
        const closeRemoveModal = function() {
            removeHashtagModal.style.display = 'none';
            document.getElementById('hashtagSelectRemove').value = '';
        };

        if (closeRemoveModalBtn) {
            closeRemoveModalBtn.addEventListener('click', closeRemoveModal);
        }

        if (cancelRemoveModalBtn) {
            cancelRemoveModalBtn.addEventListener('click', closeRemoveModal);
        }

        // Close modal when clicking outside
        removeHashtagModal.addEventListener('click', function(e) {
            if (e.target === removeHashtagModal) {
                closeRemoveModal();
            }
        });
    }

    // Sort-Funktionalität
    const sortBtn = document.getElementById('sortBtn');
    const sortDropdownBtn = document.getElementById('sortDropdownBtn');
    const sortDropdownMenu = document.getElementById('sortDropdownMenu');

    if (sortBtn && sortDropdownBtn && sortDropdownMenu) {
        // Toggle Dropdown
        sortDropdownBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            const isVisible = sortDropdownMenu.style.display === 'block';
            sortDropdownMenu.style.display = isVisible ? 'none' : 'block';
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', function(e) {
            if (!sortBtn.contains(e.target) && !sortDropdownBtn.contains(e.target) && !sortDropdownMenu.contains(e.target)) {
                sortDropdownMenu.style.display = 'none';
            }
        });

        // Main sort button click - cycle through: ASC -> DESC -> NONE
        sortBtn.addEventListener('click', function() {
            const currentField = this.dataset.currentField;
            const currentDir = this.dataset.currentDir;

            if (!currentField) {
                // No sort yet - do nothing, user should use dropdown
                return;
            }

            // Build URL with search parameter preserved
            const urlParams = new URLSearchParams(window.location.search);
            const searchTerm = urlParams.get('search');
            let newUrl = '/contacts';

            if (currentDir === 'asc') {
                // Switch to DESC
                newUrl += '?sort=' + currentField + '&dir=desc';
            } else if (currentDir === 'desc') {
                // Remove sort (go back to default)
                newUrl = '/contacts';
            }

            // Preserve search term if present
            if (searchTerm) {
                newUrl += (newUrl.includes('?') ? '&' : '?') + 'search=' + encodeURIComponent(searchTerm);
            }

            window.location.href = newUrl;
        });

        // Dropdown item clicks - select sort field
        const dropdownItems = sortDropdownMenu.querySelectorAll('.dropdown-item');
        dropdownItems.forEach(item => {
            item.addEventListener('click', function(e) {
                e.preventDefault();
                const sortField = this.dataset.sortField;

                // Build URL with search parameter preserved
                const urlParams = new URLSearchParams(window.location.search);
                const searchTerm = urlParams.get('search');
                let newUrl = '/contacts?sort=' + sortField + '&dir=asc';

                // Preserve search term if present
                if (searchTerm) {
                    newUrl += '&search=' + encodeURIComponent(searchTerm);
                }

                window.location.href = newUrl;
            });
        });
    }
});
