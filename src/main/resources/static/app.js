const API_URL = '/api';
let token = localStorage.getItem('token');
let currentUser = null;

// Headers helper
function getHeaders() {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

// Login
async function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) throw new Error('Login failed');

        const data = await response.json();
        token = data.accessToken;
        localStorage.setItem('token', token);
        currentUser = data.username;
        showDashboard();
    } catch (error) {
        alert(error.message);
    }
}

function logout() {
    token = null;
    localStorage.removeItem('token');
    showLogin();
}

function showLogin() {
    document.getElementById('loginSection').classList.remove('hidden');
    document.getElementById('dashboardSection').classList.add('hidden');
}

function showDashboard() {
    document.getElementById('loginSection').classList.add('hidden');
    document.getElementById('dashboardSection').classList.remove('hidden');
    document.getElementById('currentUser').innerText = currentUser || 'User';
}

// Load Payments
async function loadStudentPayments() {
    const studentId = document.getElementById('studentIdInput').value;
    if (!studentId) return;

    try {
        const response = await fetch(`${API_URL}/payments/student/${studentId}`, {
            headers: getHeaders()
        });

        if (!response.ok) throw new Error('Failed to fetch payments');

        const payments = await response.json();
        renderPayments(payments);
    } catch (error) {
        console.error(error);
        alert('Error loading payments. Check console.');
    }
}

// Render Payments
function renderPayments(payments) {
    const tbody = document.getElementById('paymentsTableBody');
    const payButton = document.getElementById('payButton');
    const paymentHistorySection = document.getElementById('paymentHistorySection');

    paymentHistorySection.classList.remove('hidden');
    tbody.innerHTML = '';

    // Requirement: "Agar user umuman to'lov qilmagan bo'lsa faqat to'lov qilish buttoni bo'ladi"
    // If no payments, show Pay button, hide table content (empty).
    if (payments.length === 0) {
        payButton.classList.remove('hidden');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">No payments found. Please pay.</td></tr>';
    } else {
        // Requirement: "agar u to'liq yoki qisman to'lov qilgan bo'lsa edit va delete buttoni paydo bo'lishi kerak"
        // Show payments. Edit/Delete buttons per row.
        // Hiding Pay button to strictly follow "ONLY Pay button" when no payments.
        // But practically, maybe we should keep it. I'll hide it for now to be safe.
        payButton.classList.add('hidden');

        payments.forEach(payment => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${payment.id}</td>
                <td>${payment.amount}</td>
                <td>${new Date(payment.createdAt).toLocaleDateString()}</td>
                <td>${payment.category}</td> <!-- Requirement: Paymentni turida Kartda yoki naqda mi -->
                <td>${payment.status || '-'}</td>
                <td>
                    <button class="btn btn-sm btn-warning" onclick="openEditModal(${payment.id}, ${payment.amount})">Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="deletePayment(${payment.id})">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }
}

// Edit Payment
let editingPaymentId = null;

function openEditModal(id, currentAmount) {
    editingPaymentId = id;
    document.getElementById('editPaymentId').value = id;
    document.getElementById('editPaymentAmount').value = currentAmount; // Requirement: inputda 20000 turadi
    const modal = new bootstrap.Modal(document.getElementById('editPaymentModal'));
    modal.show();
}

async function submitEditPayment() {
    const amount = document.getElementById('editPaymentAmount').value;
    if (!amount) return;

    try {
        const response = await fetch(`${API_URL}/payments/${editingPaymentId}`, {
            method: 'PUT',
            headers: getHeaders(),
            body: JSON.stringify({ amount: parseFloat(amount) })
        });

        if (!response.ok) throw new Error('Failed to update payment');

        // Close modal and reload
        const modalEl = document.getElementById('editPaymentModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        modal.hide();

        loadStudentPayments();
    } catch (error) {
        alert(error.message);
    }
}

// Delete Payment
async function deletePayment(id) {
    if (!confirm('Are you sure you want to delete this payment?')) return;

    try {
        const response = await fetch(`${API_URL}/payments/${id}`, {
            method: 'DELETE',
            headers: getHeaders()
        });

        if (!response.ok) throw new Error('Failed to delete payment');

        loadStudentPayments();
    } catch (error) {
        alert(error.message);
    }
}

function showPayModal() {
    alert('Pay functionality not implemented in this demo (focus is on Edit/Delete logic)');
}

// Initial check
if (token) {
    showDashboard();
} else {
    showLogin();
}
