import * as admin from "firebase-admin";

admin.initializeApp();

export { scheduledMessageShredder, onDeleteMessage } from "./shredder";
export { geminiProxy } from "./geminiProxy";
export { storeMessageKey } from "./storeMessageKey";
export { getMessageKey } from "./getMessageKey";
