// Inicializa Firebase (Auth + Firestore) usando el SDK modular vía CDN,
// exactamente el mismo proyecto que usa la app Android, así que todo lo que
// pase aquí (productos, ventas, cortes) se ve reflejado también en la app
// del celular y viceversa, en tiempo real.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.13.2/firebase-app.js";
import {
  getAuth,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/10.13.2/firebase-auth.js";
import {
  getFirestore,
  enableIndexedDbPersistence
} from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";

import { firebaseConfig } from "./config.js";

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);

// RNF2: persistencia offline (equivalente a la que ya trae Firestore en
// Android por defecto). Si falla (por ejemplo, varias pestañas abiertas),
// la app sigue funcionando normal, solo sin cache offline.
enableIndexedDbPersistence(db).catch(() => {});

export { signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut, onAuthStateChanged };
