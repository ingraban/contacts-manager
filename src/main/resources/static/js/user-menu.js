// User Menu Dropdown Functionality
(function() {
    const userMenuButton = document.getElementById('userMenuButton');
    const dropdown = document.getElementById('userMenuDropdown');

    if (userMenuButton && dropdown) {
        // Toggle dropdown on button click
        userMenuButton.addEventListener('click', function(event) {
            event.stopPropagation();
            dropdown.classList.toggle('show');
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', function(event) {
            if (dropdown.classList.contains('show')) {
                dropdown.classList.remove('show');
            }
        });
    }
})();
