import React, { useState, useEffect } from 'react';
import { AlertCircle, Shield, Mail, User, Phone, CheckCircle } from 'lucide-react';
import { supabase, authHelpers } from './service/SupabaseClient';
import { emergencyApi } from './service/EmergencyApi';

const EmergencyContactApp = () => {
    const [user, setUser] = useState(null);
    const [session, setSession] = useState(null);
    const [contact, setContact] = useState({
        contactName: '',
        contactFirstName: '',
        contactEmail: '',
        contactPhone: ''
    });
    const [userInformation, setUserInformation] = useState({
        firstName: '',
        lastName: ''
    });
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [userMessage, setUserMessage] = useState(null);
    const [isLogin, setIsLogin] = useState(true);
    const [credentials, setCredentials] = useState({email: '', password: ''});
    const [pairingSession, setPairingSession] = useState(null);
    const [availableDevices, setAvailableDevices] = useState([]);
    const [pairingLoading, setPairingLoading] = useState(false);
    const [confirmDevice, setConfirmDevice] = useState(null);
    const [pairedDevice, setPairedDevice] = useState(null);

    // Check für bestehende Session beim Laden
    useEffect(() => {
        checkUser();

        // Auth State Changes abonnieren
        const {data: authListener} = authHelpers.onAuthStateChange((event, session) => {
            console.log('Auth event:', event);
            setSession(session);
            setUser(session?.user ?? null);

            if (session?.user) {
                loadContact(session.access_token);
                loadUserInfo(session.access_token);
                loadPairedDevice(session.access_token);
            }
        });

        return () => {
            authListener?.subscription.unsubscribe();
        };
    }, []);

    // Prüfe ob User eingeloggt ist
    const checkUser = async () => {
        const {session} = await authHelpers.getSession();
        setSession(session);
        setUser(session?.user ?? null);

        if (session?.access_token) {
            loadContact(session.access_token);
        }
    };

    const formatLastAnnounce = (timestamp) => {
        if (!timestamp) return 'Unbekannt';
        const date = new Date(timestamp);
        const now = new Date();
        const diffSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
        console.log('timestamp:', timestamp, 'diffSeconds:', diffSeconds);
        if (diffSeconds < 10) return 'gerade eben';
        if (diffSeconds < 60) return `vor ${diffSeconds} Sekunden`;
        if (diffSeconds < 120) return 'vor 1 Minute';
        return `vor ${Math.floor(diffSeconds / 60)} Minuten`;
    };

    // Notfallkontakt laden
    const loadContact = async (token) => {
        try {
            const data = await emergencyApi.getContact(token);
            if (data) {
                setContact({
                    contactName: data.contactName || '',
                    contactFirstName: data.contactFirstName || '',
                    contactEmail: data.contactEmail || '',
                    contactPhone: data.contactPhone || ''
                });
            }
        } catch (error) {
            console.error('Fehler beim Laden:', error);
        }
    };

    //Userinformation laden
    const loadUserInfo = async (token) => {
        try {
            const data = await emergencyApi.getUserInformation(token);

            if (data) {
                setUserInformation({
                    firstName: data.firstName,
                    lastName: data.lastName
                });
            }
        } catch (error) {
            console.error('Fehler beim Laden: ', error);
        }
    };

    // Userinformationen speichern
    const saveUserInformation = async () => {
        if (!userInformation.firstName || !userInformation.lastName) {
            setUserMessage({type: 'error', text: 'Bitte Vorname und Nachname ausfüllen'});
            return;
        }

        if (!session?.access_token) {
            setUserMessage({type: 'error', text: 'Nicht angemeldet'});
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            await emergencyApi.saveUserInformation(userInformation, session.access_token);
            setUserMessage({type: 'success', text: '✅ Userinformationen gespeichert!'});
        } catch (error) {
            console.error('Save error:', error);
            setUserMessage({type: 'error', text: 'Fehler beim Speichern: ' + error.message});
        }

        setLoading(false);
    };

    // Kontakt speichern
    const saveContact = async () => {
        if (!contact.contactName || !contact.contactFirstName || !contact.contactEmail) {
            setMessage({type: 'error', text: 'Bitte Name und E-Mail ausfüllen'});
            return;
        }

        if (!session?.access_token) {
            setMessage({type: 'error', text: 'Nicht angemeldet'});
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            await emergencyApi.saveContact(contact, session.access_token);
            setMessage({type: 'success', text: '✅ Notfallkontakt gespeichert!'});
        } catch (error) {
            console.error('Save error:', error);
            setMessage({type: 'error', text: 'Fehler beim Speichern: ' + error.message});
        }

        setLoading(false);
    };

    // Trigger auslösen
    const trigger = async () => {
        setLoading(true);
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
        } catch (error) {
            console.error('Trigger error: ', error);
            setMessage({type: 'error', text: 'Fehler beim Triggern: ' + error.message})
        }
        setLoading(false);
    }

    // Pairing auslösen
    const pairing = async () => {
        setPairingLoading(true);
        try {
            const pairingSession = await emergencyApi.startPairing(session.access_token);
            const devices = await emergencyApi.getAvailableDevices(session.access_token);
            setPairingSession(pairingSession);
            setAvailableDevices(devices);
        } catch (error) {
            console.error('Pairing error: ', error);
            setMessage({type: 'error', text: 'Fehler beim Pairing: ' + error.message});
        }
        setPairingLoading(false);
    };

    const handleDeviceClick = async (device) => {
        setPairingLoading(true);
        setConfirmDevice(device);
        try {
            const pairingSession = await emergencyApi.pairWithDevice(device.deviceId, session.access_token);
            setPairingSession(pairingSession);
        } catch (error) {
            console.error('Device pairing error', error);
            setMessage({type: 'error', text: 'Fehler beim Pairing mit Device: ' + error.message});
        }
        setPairingLoading(false);
    }

    const handleConfirmPairing = async () => {
        try {
            await emergencyApi.confirmPairing(confirmDevice.deviceId, session.access_token);
            setPairedDevice(confirmDevice);
            setConfirmDevice(null);
            setPairingSession(null);
            setMessage({type: 'success', text: '✅ Gerät erfolgreich gekoppelt!'});
        } catch (error) {
            setMessage({type: 'error', text: 'Fehler beim Koppeln: ' + error.message});
        }
    };

    const loadPairedDevice = async (token) => {
        try {
            const device = await emergencyApi.getDeviceForUser(token);
            setPairedDevice(device);
        } catch (error) {
            console.error('Fehler beim Laden des Geräts:', error);
        }
    };

    // Login/Registrierung
    const handleAuth = async () => {
        if (!credentials.email || !credentials.password) {
            setMessage({type: 'error', text: 'Bitte E-Mail und Passwort eingeben'});
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
                setMessage({type: 'success', text: '✅ Erfolgreich angemeldet!'});
                setSession(result.data.session);
                setUser(result.data.user);
            } else {
                setMessage({
                    type: 'success',
                    text: '✅ Registrierung erfolgreich! Bitte bestätige deine E-Mail-Adresse.'
                });
                setIsLogin(true);
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
        setContact({contactName: '', contactFirstName: '', contactEmail: '', contactPhone: ''});
        setUserInformation({firstName: '', lastName: ''});
        setMessage({type: 'success', text: 'Erfolgreich abgemeldet'});
        setUserMessage(null);
        setCredentials({email: '', password: ''});
    };

    if (!user) {
        return (
            <div
                className="min-h-screen bg-gradient-to-br from-red-50 to-orange-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
                    <div className="text-center mb-8">
                        <div className="inline-flex items-center justify-center w-16 h-16 bg-red-100 rounded-full mb-4">
                            <Shield className="w-8 h-8 text-red-600"/>
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
                        <div
                            className={`mt-4 p-3 rounded-lg ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
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

                {/* Bestätigungsdialog */}
                {confirmDevice && (
                    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 p-4">
                        <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-sm">
                            <div className="text-center mb-6">
                                <div
                                    className="inline-flex items-center justify-center w-14 h-14 bg-red-100 rounded-full mb-4">
                                    <Shield className="w-7 h-7 text-red-600"/>
                                </div>
                                <h3 className="text-lg font-bold text-gray-900 mb-2">Gerät koppeln</h3>
                                <p className="text-sm text-gray-600 mb-4">
                                    Wird auf Ihrem Gerät der folgende Code angezeigt?
                                </p>
                                <div className="bg-gray-50 rounded-xl py-4 px-6 mb-4">
                                    <p className="text-4xl font-mono font-bold tracking-widest text-gray-900">
                                        {pairingSession?.code}
                                    </p>
                                </div>
                                <p className="text-xs text-gray-400 font-mono">{confirmDevice.deviceId}</p>
                            </div>

                            <div className="flex gap-3">
                                <button
                                    onClick={() => setConfirmDevice(null)}
                                    className="flex-1 py-3 rounded-lg border border-gray-300 text-sm font-medium text-gray-700 hover:bg-gray-50 transition"
                                >
                                    Abbrechen
                                </button>
                                <button
                                    onClick={handleConfirmPairing}
                                    className="flex-1 py-3 rounded-lg bg-red-600 text-white text-sm font-semibold hover:bg-red-700 transition"
                                >
                                    Ja, koppeln
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Header */}
                <div className="bg-white rounded-2xl shadow-lg p-6 mb-6">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
                                <Shield className="w-6 h-6 text-red-600"/>
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

                {/* Eigene Daten */}
                <div className="bg-white rounded-2xl shadow-lg p-8 mb-6">
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-2">
                            <AlertCircle className="w-5 h-5 text-red-600"/>
                            <h2 className="text-xl font-bold text-gray-900">Eigene Daten hinterlegen:</h2>
                        </div>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <User className="w-4 h-4"/>
                                Vorname
                            </label>
                            <input
                                type="text"
                                value={userInformation.firstName}
                                onChange={(e) => setUserInformation({...userInformation, firstName: e.target.value})}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="Max"
                            />
                        </div>

                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <User className="w-4 h-4"/>
                                Nachname
                            </label>
                            <input
                                type="text"
                                value={userInformation.lastName}
                                onChange={(e) => setUserInformation({...userInformation, lastName: e.target.value})}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="Mustermann"
                            />
                        </div>

                        <button
                            onClick={saveUserInformation}
                            disabled={loading}
                            className="w-full bg-red-600 text-white py-3 rounded-lg font-semibold hover:bg-red-700 transition disabled:opacity-50 flex items-center justify-center gap-2"
                        >
                            {loading ? 'Speichere...' : (
                                <>
                                    <CheckCircle className="w-5 h-5"/>
                                    Userdaten speichern
                                </>
                            )}
                        </button>

                        {userMessage && (
                            <div
                                className={`mt-4 p-3 rounded-lg ${userMessage.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
                                {userMessage.text}
                            </div>
                        )}
                    </div>
                </div>

                {/* Notfallkontakt */}
                <div className="bg-white rounded-2xl shadow-lg p-8 mb-6">
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-2">
                            <AlertCircle className="w-5 h-5 text-red-600"/>
                            <h2 className="text-xl font-bold text-gray-900">Notfallkontakt hinterlegen</h2>
                        </div>
                        <p className="text-gray-600">Diese Person wird bei einem Notfall per E-Mail benachrichtigt.</p>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <User className="w-4 h-4"/>
                                Vorname
                            </label>
                            <input
                                type="text"
                                value={contact.contactFirstName}
                                onChange={(e) => setContact({...contact, contactFirstName: e.target.value})}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="Max"
                            />
                        </div>

                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <User className="w-4 h-4"/>
                                Nachname
                            </label>
                            <input
                                type="text"
                                value={contact.contactName}
                                onChange={(e) => setContact({...contact, contactName: e.target.value})}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="Mustermann"
                            />
                        </div>

                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <Mail className="w-4 h-4"/>
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
                                <Phone className="w-4 h-4"/>
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
                                    <CheckCircle className="w-5 h-5"/>
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
                        <div
                            className={`mt-6 p-4 rounded-lg ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
                            {message.text}
                        </div>
                    )}
                </div>

                {/* Pairing */}
                <div className="bg-white rounded-2xl shadow-lg p-8 mb-6">
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-2">
                            <AlertCircle className="w-5 h-5 text-red-600" />
                            <h2 className="text-xl font-bold text-gray-900">Gerät koppeln</h2>
                        </div>
                        <p className="text-gray-600">Verbinde deinen Rollstuhl mit deinem Account.</p>
                    </div>

                    {pairedDevice ? (
                        // Gekoppeltes Gerät anzeigen
                        <div>
                            <div className="bg-green-50 border border-green-200 rounded-xl p-5 mb-4">
                                <div className="flex items-center gap-3 mb-3">
                                    <div className="w-3 h-3 rounded-full bg-green-500"></div>
                                    <p className="text-sm font-semibold text-green-800">Gerät verbunden</p>
                                </div>
                                <p className="font-mono text-sm text-gray-700 mb-1">{pairedDevice.deviceId}</p>
                                <p className="text-xs text-gray-400">
                                    Zuletzt gesehen: {formatLastAnnounce(pairedDevice.lastAnnounce)}
                                </p>
                            </div>
                            <button
                                onClick={async () => {
                                    try {
                                        await emergencyApi.unpairDevice(pairedDevice.deviceId, session.access_token);
                                        setPairedDevice(null);
                                        setMessage({ type: 'success', text: '✅ Gerät erfolgreich entkoppelt.' });
                                    } catch (error) {
                                        setMessage({ type: 'error', text: 'Fehler beim Entkoppeln: ' + error.message });
                                    }
                                }}
                                className="w-full py-3 rounded-lg border border-red-300 text-red-600 text-sm font-medium hover:bg-red-50 transition"
                            >
                                Gerät entkoppeln
                            </button>
                        </div>
                    ) : pairingSession ? (
                        <div>
                            {/* Code-Anzeige */}
                            <div className="bg-gray-50 rounded-xl p-6 text-center mb-6">
                                <p className="text-sm text-gray-500 mb-2">Dein Pairing-Code</p>
                                <p className="text-5xl font-mono font-bold tracking-widest text-gray-900">
                                    {pairingSession.code}
                                </p>
                                <p className="text-xs text-gray-400 mt-2">Gib diesen Code am Gerät ein – gültig für 2
                                    Minuten</p>
                            </div>

                            {/* Geräteliste */}
                            <p className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-3">
                                Verfügbare Geräte
                            </p>

                            {availableDevices.length === 0 ? (
                                <div
                                    className="border border-dashed border-gray-300 rounded-xl p-8 text-center text-sm text-gray-400">
                                    Keine Geräte gefunden – warte bis sich ein Gerät ankündigt
                                </div>
                            ) : (
                                <div className="space-y-2">
                                    {availableDevices.map(device => (
                                        <div
                                            key={device.deviceId}
                                            onClick={() => handleDeviceClick(device)}
                                            className="flex items-center justify-between bg-white border border-gray-200 rounded-xl p-4 cursor-pointer hover:bg-gray-50 active:scale-[0.98] transition transform"
                                        >
                                            <div className="flex items-center gap-3">
                                                <div className="w-2 h-2 rounded-full bg-green-500 flex-shrink-0"></div>
                                                <div>
                                                    <p className="font-mono text-sm text-gray-900">{device.deviceId}</p>
                                                    <p className="text-xs text-gray-400">Zuletzt
                                                        gesehen: {formatLastAnnounce(device.lastAnnounce)}</p>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            <button
                                onClick={() => setPairingSession(null)}
                                className="mt-4 w-full py-2 text-sm text-gray-500 hover:text-gray-800"
                            >
                                Abbrechen
                            </button>
                        </div>
                    ) : (
                        <button
                            onClick={pairing}
                            disabled={pairingLoading}
                            className="w-full bg-red-600 text-white py-3 rounded-lg font-semibold hover:bg-red-700 transition disabled:opacity-50 flex items-center justify-center gap-2"
                        >
                            {pairingLoading ? 'Pairing wird begonnen...' : 'Pairing starten'}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}

export default EmergencyContactApp;