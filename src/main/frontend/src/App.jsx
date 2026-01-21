import React, { useState, useEffect } from 'react';
import { AlertCircle, Shield, Mail, User, Phone, CheckCircle } from 'lucide-react';
import { supabase, authHelpers } from './service/SupabaseClient';
import { emergencyApi } from './service/EmergencyApi';

const EmergencyContactApp = () => {
    const [user, setUser] = useState(null);
    const [session, setSession] = useState(null);
    const [contact, setContact] = useState({
        contactName: '',
        contactEmail: '',
        contactPhone: ''
    });
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [isLogin, setIsLogin] = useState(true);
    const [credentials, setCredentials] = useState({ email: '', password: '' });

    // Check für bestehende Session beim Laden
    useEffect(() => {
        checkUser();

        // Auth State Changes abonnieren
        const { data: authListener } = authHelpers.onAuthStateChange((event, session) => {
            console.log('Auth event:', event);
            setSession(session);
            setUser(session?.user ?? null);

            if (session?.user) {
                loadContact(session.access_token);
            }
        });

        return () => {
            authListener?.subscription.unsubscribe();
        };
    }, []);

    // Prüfe ob User eingeloggt ist
    const checkUser = async () => {
        const { session } = await authHelpers.getSession();
        setSession(session);
        setUser(session?.user ?? null);

        if (session?.access_token) {
            loadContact(session.access_token);
        }
    };

    // Notfallkontakt laden
    const loadContact = async (token) => {
        try {
            const data = await emergencyApi.getContact(token);
            if (data) {
                setContact({
                    contactName: data.contactName || '',
                    contactEmail: data.contactEmail || '',
                    contactPhone: data.contactPhone || ''
                });
            }
        } catch (error) {
            console.error('Fehler beim Laden:', error);
        }
    };

    // Kontakt speichern
    const saveContact = async () => {
        if (!contact.contactName || !contact.contactEmail) {
            setMessage({ type: 'error', text: 'Bitte Name und E-Mail ausfüllen' });
            return;
        }

        if (!session?.access_token) {
            setMessage({ type: 'error', text: 'Nicht angemeldet' });
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            await emergencyApi.saveContact(contact, session.access_token);
            setMessage({ type: 'success', text: '✅ Notfallkontakt gespeichert!' });
        } catch (error) {
            console.error('Save error:', error);
            setMessage({ type: 'error', text: 'Fehler beim Speichern: ' + error.message });
        }

        setLoading(false);
    };

    // Trigger auslösen
    const trigger = async () => {
        try {
            const contact = await emergencyApi.getContact(session.access_token);
            if (!contact.userId) {
                setMessage({type: 'error', text: 'Es wurde kein Notfallkontakt gefunden.'});
            } else {
                await emergencyApi.triggerEmergency(session.access_token);
                setMessage({
                    type: 'success',
                    text: '✅ Trigger wurde erfolgreich ausgelöst und Test-Email wurde versendet.'
                });
            }
        }catch (error){
                console.error('Trigger error: ', error);
                setMessage({ type: 'error', text: 'Fehler beim Triggern: ' + error.message })
        }
    }

    // Login/Registrierung
    const handleAuth = async () => {
        if (!credentials.email || !credentials.password) {
            setMessage({ type: 'error', text: 'Bitte E-Mail und Passwort eingeben' });
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            let result;

            if (isLogin) {
                // Anmelden
                result = await authHelpers.signIn(credentials.email, credentials.password);
            } else {
                // Registrieren
                result = await authHelpers.signUp(credentials.email, credentials.password);
            }

            if (result.error) {
                throw new Error(result.error.message);
            }

            if (isLogin) {
                setMessage({ type: 'success', text: '✅ Erfolgreich angemeldet!' });
                setSession(result.data.session);
                setUser(result.data.user);
            } else {
                setMessage({
                    type: 'success',
                    text: '✅ Registrierung erfolgreich! Bitte bestätige deine E-Mail-Adresse.'
                });
            }

        } catch (error) {
            console.error('Auth error:', error);
            setMessage({
                type: 'error',
                text: isLogin ? 'Anmeldung fehlgeschlagen: ' + error.message : 'Registrierung fehlgeschlagen: ' + error.message
            });
        }

        setLoading(false);
    };

    // Logout
    const handleLogout = async () => {
        await authHelpers.signOut();
        setUser(null);
        setSession(null);
        setContact({ contactName: '', contactEmail: '', contactPhone: '' });
        setMessage({ type: 'success', text: 'Erfolgreich abgemeldet' });
    };

    if (!user) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-red-50 to-orange-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
                    <div className="text-center mb-8">
                        <div className="inline-flex items-center justify-center w-16 h-16 bg-red-100 rounded-full mb-4">
                            <Shield className="w-8 h-8 text-red-600" />
                        </div>
                        <h1 className="text-3xl font-bold text-gray-900">Notfallkontakt</h1>
                        <p className="text-gray-600 mt-2">Sicher und zuverlässig</p>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">E-Mail</label>
                            <input
                                type="email"
                                value={credentials.email}
                                onChange={(e) => setCredentials({...credentials, email: e.target.value})}
                                onKeyPress={(e) => e.key === 'Enter' && handleAuth()}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="deine@email.de"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Passwort</label>
                            <input
                                type="password"
                                value={credentials.password}
                                onChange={(e) => setCredentials({...credentials, password: e.target.value})}
                                onKeyPress={(e) => e.key === 'Enter' && handleAuth()}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="••••••••"
                            />
                        </div>

                        <button
                            onClick={handleAuth}
                            disabled={loading}
                            className="w-full bg-red-600 text-white py-3 rounded-lg font-semibold hover:bg-red-700 transition disabled:opacity-50"
                        >
                            {loading ? 'Lädt...' : isLogin ? 'Anmelden' : 'Registrieren'}
                        </button>

                        <button
                            onClick={() => setIsLogin(!isLogin)}
                            className="w-full text-sm text-gray-600 hover:text-gray-900"
                        >
                            {isLogin ? 'Noch kein Account? Registrieren' : 'Bereits registriert? Anmelden'}
                        </button>
                    </div>

                    {message && (
                        <div className={`mt-4 p-3 rounded-lg ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
                            {message.text}
                        </div>
                    )}

                    <div className="mt-6 p-4 bg-blue-50 rounded-lg text-sm text-blue-800">
                        <strong>ℹ️ Info:</strong> Bei Registrierung wird eine Bestätigungs-E-Mail versendet.
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-red-50 to-orange-50 p-4">
            <div className="max-w-2xl mx-auto">
                {/* Header */}
                <div className="bg-white rounded-2xl shadow-lg p-6 mb-6">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
                                <Shield className="w-6 h-6 text-red-600" />
                            </div>
                            <div>
                                <h1 className="text-2xl font-bold text-gray-900">Notfallkontakt</h1>
                                <p className="text-sm text-gray-600">{user.email}</p>
                            </div>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg"
                        >
                            Abmelden
                        </button>
                    </div>
                </div>

                {/* Hauptformular */}
                <div className="bg-white rounded-2xl shadow-lg p-8">
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-2">
                            <AlertCircle className="w-5 h-5 text-red-600" />
                            <h2 className="text-xl font-bold text-gray-900">Notfallkontakt hinterlegen</h2>
                        </div>
                        <p className="text-gray-600">Diese Person wird bei einem Notfall per E-Mail benachrichtigt.</p>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <User className="w-4 h-4" />
                                Name des Kontakts
                            </label>
                            <input
                                type="text"
                                value={contact.contactName}
                                onChange={(e) => setContact({...contact, contactName: e.target.value})}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="Max Mustermann"
                            />
                        </div>

                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <Mail className="w-4 h-4" />
                                E-Mail-Adresse
                            </label>
                            <input
                                type="email"
                                value={contact.contactEmail}
                                onChange={(e) => setContact({...contact, contactEmail: e.target.value})}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="max@beispiel.de"
                            />
                        </div>

                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <Phone className="w-4 h-4" />
                                Telefonnummer (optional)
                            </label>
                            <input
                                type="tel"
                                value={contact.contactPhone}
                                onChange={(e) => setContact({...contact, contactPhone: e.target.value})}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="+49 123 456789"
                            />
                        </div>

                        <button
                            onClick={saveContact}
                            disabled={loading}
                            className="w-full bg-red-600 text-white py-3 rounded-lg font-semibold hover:bg-red-700 transition disabled:opacity-50 flex items-center justify-center gap-2"
                        >
                            {loading ? 'Speichere...' : (
                                <>
                                    <CheckCircle className="w-5 h-5" />
                                    Notfallkontakt speichern
                                </>
                            )}
                        </button>

                        <button
                            onClick={trigger}
                            disabled={loading}
                            className="w-full bg-red-600 text-white py-3 rounded-lg font-semibold hover:bg-red-700 transition disabled:opacity-50 flex items-center justify-center gap-2"
                        >
                            {loading ? 'Trigger wird ausgelöst...' : 'Triggern'}
                        </button>
                    </div>

                    {message && (
                        <div className={`mt-6 p-4 rounded-lg ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
                            {message.text}
                        </div>
                    )}
                </div>

                {/* Info Box */}
                <div className="mt-6 bg-blue-50 border border-blue-200 rounded-xl p-6">
                    <h3 className="font-semibold text-blue-900 mb-2">🔐 Wie funktioniert das?</h3>
                    <ul className="text-sm text-blue-800 space-y-1">
                        <li>• Deine Daten werden verschlüsselt bei Supabase gespeichert</li>
                        <li>• Der Notfall-Endpoint kann per HTTP POST ausgelöst werden</li>
                        <li>• Bei Auslösung wird automatisch eine E-Mail versendet</li>
                        <li>• Test-E-Mail wird beim Speichern versendet</li>
                    </ul>
                </div>
            </div>
        </div>
    );
};

export default EmergencyContactApp;