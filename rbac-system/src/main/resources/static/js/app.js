/**
 * Role-Based Access Control (RBAC) System - Client-Side App Script
 * Features:
 * - Collapsible Sidebar (Desktop & Mobile Drawer with localStorage)
 * - Password Show/Hide Toggle
 * - Stat Cards Count-Up Animation
 * - Toast Notifications Auto-Dismiss
 * - Client-Side Table Search, Column Sorting, & Pagination
 * - Bootstrap Tooltip Initialization
 */

document.addEventListener('DOMContentLoaded', function () {
    initSidebar();
    initPasswordToggles();
    initCountUpAnimations();
    initToasts();
    initTableEnhancements();
    initTooltips();
    initModals();
});

/* =====================================================================
   1. Sidebar Management
   ===================================================================== */
function initSidebar() {
    const sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
    const mobileToggleBtn = document.getElementById('sidebarToggle');
    const appSidebar = document.querySelector('.app-sidebar');

    // Restore desktop collapsed state
    if (localStorage.getItem('sidebar_collapsed') === 'true') {
        document.body.classList.add('sidebar-collapsed');
    }

    if (sidebarCollapseBtn) {
        sidebarCollapseBtn.addEventListener('click', function () {
            document.body.classList.toggle('sidebar-collapsed');
            const isCollapsed = document.body.classList.contains('sidebar-collapsed');
            localStorage.setItem('sidebar_collapsed', isCollapsed ? 'true' : 'false');
        });
    }

    if (mobileToggleBtn && appSidebar) {
        mobileToggleBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            appSidebar.classList.toggle('show');
        });

        // Close sidebar when clicking outside on mobile
        document.addEventListener('click', function (e) {
            if (window.innerWidth < 992) {
                if (!appSidebar.contains(e.target) && !mobileToggleBtn.contains(e.target) && appSidebar.classList.contains('show')) {
                    appSidebar.classList.remove('show');
                }
            }
        });
    }
}

/* =====================================================================
   2. Password Show / Hide Toggle
   ===================================================================== */
function initPasswordToggles() {
    document.querySelectorAll('.password-toggle-btn, .toggle-password').forEach(btn => {
        btn.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            const input = targetId ? document.getElementById(targetId) : this.closest('.input-group')?.querySelector('input[type="password"], input[type="text"]');
            if (!input) return;

            const icon = this.querySelector('i');
            if (input.type === 'password') {
                input.type = 'text';
                if (icon) {
                    icon.classList.remove('bi-eye', 'bi-eye-fill');
                    icon.classList.add('bi-eye-slash-fill');
                }
            } else {
                input.type = 'password';
                if (icon) {
                    icon.classList.remove('bi-eye-slash-fill');
                    icon.classList.add('bi-eye-fill');
                }
            }
        });
    });
}

/* =====================================================================
   3. Stat Cards Count-Up Animation
   ===================================================================== */
function initCountUpAnimations() {
    const counters = document.querySelectorAll('.stat-count-up');
    counters.forEach(counter => {
        const target = parseInt(counter.getAttribute('data-target') || counter.innerText.replace(/[^0-9]/g, ''), 10);
        if (isNaN(target)) return;

        let current = 0;
        const duration = 1000; // 1s
        const stepTime = 20;
        const totalSteps = duration / stepTime;
        const increment = target / totalSteps;

        counter.innerText = '0';
        const timer = setInterval(() => {
            current += increment;
            if (current >= target) {
                counter.innerText = target.toLocaleString();
                clearInterval(timer);
            } else {
                counter.innerText = Math.floor(current).toLocaleString();
            }
        }, stepTime);
    });
}

/* =====================================================================
   4. Toast Notifications Auto-Dismiss
   ===================================================================== */
function initToasts() {
    document.querySelectorAll('.toast-dark, .toast-alert').forEach(toast => {
        setTimeout(() => {
            toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(20px)';
            setTimeout(() => toast.remove(), 400);
        }, 4500);
    });
}

/* =====================================================================
   5. Client-Side Table Enhancements: Search, Sorting, & Pagination
   ===================================================================== */
function initTableEnhancements() {
    const tables = document.querySelectorAll('table.table-theme, table.table-enhance');

    tables.forEach((table, tableIdx) => {
        const tbody = table.querySelector('tbody');
        if (!tbody) return;

        // Collect rows excluding empty-state rows
        const originalRows = Array.from(tbody.querySelectorAll('tr')).filter(r => !r.querySelector('td[colspan]'));
        if (originalRows.length === 0) return;

        let visibleRows = [...originalRows];
        const rowsPerPage = 8;
        let currentPage = 1;

        // Container wrapper
        const parentCard = table.closest('.card, .card-theme');
        if (!parentCard) return;

        // 1. Create Toolbar with Live Search
        const toolbar = document.createElement('div');
        toolbar.className = 'd-flex justify-content-between align-items-center p-3 flex-wrap gap-2 table-toolbar';
        toolbar.style.borderBottom = '1px solid #2a2a2a';
        toolbar.innerHTML = `
            <div class="table-search-wrap">
                <i class="bi bi-search table-search-icon"></i>
                <input type="text" class="form-control table-search-input" placeholder="Search records...">
            </div>
            <div class="small text-muted table-row-counter">
                Showing <span class="fw-bold text-white text-count">${originalRows.length}</span> entries
            </div>
        `;
        const container = table.closest('.table-responsive') || table;
        parentCard.insertBefore(toolbar, container);

        const searchInput = toolbar.querySelector('.table-search-input');
        const countSpan = toolbar.querySelector('.text-count');

        // 2. Pagination Footer Container
        const paginationWrap = document.createElement('div');
        paginationWrap.className = 'd-flex justify-content-between align-items-center p-3 flex-wrap gap-2 table-pagination';
        paginationWrap.style.borderTop = '1px solid #2a2a2a';
        paginationWrap.innerHTML = `
            <div class="small text-muted page-info"></div>
            <ul class="pagination pagination-sm mb-0"></ul>
        `;
        parentCard.appendChild(paginationWrap);

        const pageInfo = paginationWrap.querySelector('.page-info');
        const paginationUl = paginationWrap.querySelector('.pagination');

        function renderPage() {
            const totalPages = Math.max(1, Math.ceil(visibleRows.length / rowsPerPage));
            if (currentPage > totalPages) currentPage = totalPages;

            // Detach and re-append slice
            originalRows.forEach(r => r.style.display = 'none');
            const startIdx = (currentPage - 1) * rowsPerPage;
            const endIdx = startIdx + rowsPerPage;

            visibleRows.slice(startIdx, endIdx).forEach(r => r.style.display = '');

            countSpan.innerText = visibleRows.length;
            pageInfo.innerText = visibleRows.length > 0 
                ? `Showing ${startIdx + 1} to ${Math.min(endIdx, visibleRows.length)} of ${visibleRows.length}` 
                : 'No matching records';

            // Render pagination links
            paginationUl.innerHTML = '';
            if (totalPages <= 1) return;

            // Prev
            const prevLi = document.createElement('li');
            prevLi.className = `page-item ${currentPage === 1 ? 'disabled' : ''}`;
            prevLi.innerHTML = `<button type="button" class="page-link">&laquo;</button>`;
            prevLi.onclick = () => { if (currentPage > 1) { currentPage--; renderPage(); } };
            paginationUl.appendChild(prevLi);

            // Numbers
            for (let i = 1; i <= totalPages; i++) {
                const li = document.createElement('li');
                li.className = `page-item ${currentPage === i ? 'active' : ''}`;
                li.innerHTML = `<button type="button" class="page-link">${i}</button>`;
                li.onclick = () => { currentPage = i; renderPage(); };
                paginationUl.appendChild(li);
            }

            // Next
            const nextLi = document.createElement('li');
            nextLi.className = `page-item ${currentPage === totalPages ? 'disabled' : ''}`;
            nextLi.innerHTML = `<button type="button" class="page-link">&raquo;</button>`;
            nextLi.onclick = () => { if (currentPage < totalPages) { currentPage++; renderPage(); } };
            paginationUl.appendChild(nextLi);
        }

        // Live Search Handler
        searchInput.addEventListener('input', function () {
            const query = this.value.toLowerCase().trim();
            visibleRows = originalRows.filter(row => {
                const text = row.innerText.toLowerCase();
                return text.includes(query);
            });
            currentPage = 1;
            renderPage();
        });

        // 3. Sorting on Column Headers
        const ths = table.querySelectorAll('thead th');
        ths.forEach((th, colIdx) => {
            // Skip action columns
            const text = th.innerText.trim();
            if (text.toLowerCase() === 'actions' || text.toLowerCase() === 'action') return;

            th.classList.add('sortable-th');
            th.title = 'Click to sort';
            const icon = document.createElement('i');
            icon.className = 'bi bi-arrow-down-up sort-icon';
            th.appendChild(icon);

            let asc = true;
            th.addEventListener('click', function () {
                ths.forEach(otherTh => {
                    const otherIcon = otherTh.querySelector('.sort-icon');
                    if (otherIcon) otherIcon.className = 'bi bi-arrow-down-up sort-icon';
                });

                visibleRows.sort((rowA, rowB) => {
                    const cellA = rowA.children[colIdx]?.innerText.trim() || '';
                    const cellB = rowB.children[colIdx]?.innerText.trim() || '';
                    const numA = parseFloat(cellA.replace(/[^0-9.-]/g, ''));
                    const numB = parseFloat(cellB.replace(/[^0-9.-]/g, ''));

                    if (!isNaN(numA) && !isNaN(numB)) {
                        return asc ? numA - numB : numB - numA;
                    }
                    return asc ? cellA.localeCompare(cellB) : cellB.localeCompare(cellA);
                });

                icon.className = asc ? 'bi bi-sort-alpha-down sort-icon text-danger' : 'bi bi-sort-alpha-up-alt sort-icon text-danger';
                asc = !asc;
                renderPage();
            });
        });

        renderPage();
    });
}

/* =====================================================================
   6. Bootstrap Tooltip Initialization
   ===================================================================== */
function initTooltips() {
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
        document.querySelectorAll('[data-bs-toggle="tooltip"], [title]').forEach(el => {
            if (!el.getAttribute('data-bs-toggle') && el.getAttribute('title')) {
                el.setAttribute('data-bs-toggle', 'tooltip');
            }
            try {
                new bootstrap.Tooltip(el);
            } catch (ignored) {}
        });
    }
}

/* =====================================================================
   7. Bootstrap Modal Initialization & Backdrop Cleanup
   ===================================================================== */
function initModals() {
    // Ensure clean backdrop removal when any modal finishes hiding
    document.addEventListener('hidden.bs.modal', function () {
        if (!document.querySelector('.modal.show')) {
            document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('padding-right');
            document.body.style.removeProperty('overflow');
        }
    });
}
