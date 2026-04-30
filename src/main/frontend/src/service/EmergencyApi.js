// API Helper für Backend-Kommunikation

// Für Entwicklung: Vite Proxy leitet /api zu localhost:8080
// Für Production: Gleiche Domain wie Frontend
const API_BASE_URL = '/api/emergency';

// Helper für API Requests mit Auth Token
async function fetchWithAuth(url, token, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Netzwerkfehler' }));
        throw new Error(error.error || `HTTP ${response.status}`);
    }

    return response.json();
}

export const emergencyApi = {
    // Notfallkontakt speichern
    async saveContact(contactData, token) {
        return fetchWithAuth(`${API_BASE_URL}/contact`, token, {
            method: 'POST',
            body: JSON.stringify(contactData),
        });
    },

    // Notfallkontakt abrufen
    async getContact(token) {
        return fetchWithAuth(`${API_BASE_URL}/contact`, token, {
            method: 'GET'
        });
    },

    //User Information laden
    async getUserInformation(token) {
        return fetchWithAuth(`${API_BASE_URL}/userinformation`, token, {
            method: 'GET'
        });
    },

    //User Information speichern
    async saveUserInformation(userInformationData, token) {
        return fetchWithAuth(`${API_BASE_URL}/userinformation`, token, {
            method: 'POST',
            body: JSON.stringify(userInformationData)
        });
    },

    // Notfall auslösen
    async triggerEmergency(token) {
        return await fetchWithAuth(`${API_BASE_URL}/trigger`, token, {
            method: 'POST'
        });
    },

    async startPairing(token){
        return await fetchWithAuth(`${API_BASE_URL}/user/pairing/start`, token, {
            method: 'POST'
        });
    },

    async getAvailableDevices(token) {
        return await fetchWithAuth(`${API_BASE_URL}/device/pairing`, token, {
            method: 'GET'
        })
    },

    async pairWithDevice(deviceId, token) {
        return await fetchWithAuth(`${API_BASE_URL}/user/pairing/device`, token, {
            method: 'POST',
            body: JSON.stringify({deviceId})
        });
    },

    // Health Check
    async healthCheck() {
        const response = await fetch(`${API_BASE_URL}/health`);

        if (!response.ok) {
            const error = await response.json().catch(() => ({ error: 'Netzwerkfehler' }));
            throw new Error(error.error || `HTTP ${response.status}`);
        }

        return response.json();
    }
};