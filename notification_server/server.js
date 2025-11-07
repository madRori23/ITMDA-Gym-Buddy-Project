// server.js

// --- 1. Import necessary libraries ---
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const admin = require('firebase-admin');

// --- 2. Initialize Firebase Admin SDK ---
// Make sure the path to your service account key is correct.
var serviceAccount = require("./service-account-key.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

//Initialize FireStore DB
const db = admin.firestore();

// --- 3. Initialize Express App ---
const app = express();
app.use(cors()); // Enable Cross-Origin Resource Sharing
app.use(express.json({
  limit: '10mb',
  type: function(req) {
    // Accept any content-type that contains 'json'
    return req.get('content-type')?.includes('json') || false;
  }
})); 
// Allow the app to parse JSON from request bodies
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

//fallback parsers: manually parse body data
app.use((req, res, next) => {
  if (!req.body || Object.keys(req.body).length === 0) {
    let data = '';
    req.on('data', chunk => {
      data += chunk.toString();
    });
    req.on('end', () => {
      try {
        if (data) {
          req.body = JSON.parse(data);
          console.log('Manually parsed body:', req.body);
        }
      } catch (e) {
        console.error('Failed to parse body:', e.message);
      }
      next();
    });
  } else {
    next();
  }
});

//--Error handling for invalid JSON
// Add error handling for invalid JSON
app.use((err, req, res, next) => {
  if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
    console.error('Bad JSON');
    return res.status(400).send({ error: 'Invalid JSON' });
  }
  next();
});

//Debugging middleware to log incoming requests
app.use((req, res, next) => {
  console.log('=== Incoming Request ===');
  console.log('Method:', req.method);
  console.log('Path:', req.path);
  console.log('Content-Type:', req.get('content-type'));
  console.log('Body:', req.body);
  console.log('Body Keys', Object.keys(req.body || {}));
  console.log('Raw Body:', req.body ? JSON.stringify(req.body) : 'undefined');
  console.log('=======================');
  next();
});
app.use(bodyParser.urlencoded({ extended: true }));

// --- 4. In-Memory Database (for demonstration) ---
// In a real app, you would use a proper database like Firestore, MongoDB, or PostgreSQL.
const users = {}; // We will store users as { userId: { fcmToken: '...', deviceType: '...' } }

// --- 5. Define API Endpoints ---

// Will use mutilple endpoinnts.
// Your Java code posts to the same BASE_URL for both.
//Device Registration endpoint
/*app.post('/register', (req, res) => {
  const { userId, fcmToken, deviceType } = req.body;

  // Endpoint Logic: Check if it's a registration or a message request.

  // A) --- Registration validation ---
  if (!userId || !fcmToken) {
    console.log("Registration request missing userId or fcmToken.");
    return res.status(400).send({ error: "Missing userId or fcmToken in request body." });
  } 

  // B) ---FCM Token validation---
  if(typeof fcmToken !== 'string'|| fcmToken.length< 50){
    console.log("Invalid FCM token format.");
    return res.status(400).send({ error: "Invalid FCM token format." });
  }

  // C) --- Register the user ---
  console.log(`Registering user: ${userId} with deviceType: ${deviceType}`);

  //Store users into database
  users[userId] = {
    fcmToken: fcmToken,
    deviceType: deviceType || 'unknown',
    registeredAt: new Date().toISOString()
  }

  //Get current registered users
  console.log("Current registered users:", Object.keys(users));
  res.status(200).json({ success:true,
    message: `User '${userId}' registered successfully.`,
    userId : userId
  });
});
*/

app.post('/send', async (req, res) => {

  const { targetUser, title, message } = req.body;

  // A) --- Message validation ---
  if (!targetUser || !title || !message) {
    console.error("Send failed: Missing required fields.");
    return res.status(400).send({ error: "Missing required fields" });
  }

  // B) --- Check if the user is registered || has a FCMToken ---
  console.log(`Sending message to user: ${targetUser}`);

  //C) --- Get the userInfo from database
  let targetUserInfo;

  try{
    const userDoc = await db.collection('users').doc(targetUser).get();
    if(!userDoc.exists){
      console.error(`Send failed: User '${targetUser}' not found in database`);
      return res.status(404).send({ error: `User '${targetUser}' not registered` });
    }

    targetUserInfo = userDoc.data();
  }catch (error){
    console.error(`Error reading from firestore`);
    return res.status(500).send({ error: `Failed to get user from databse` });
  }

  if(!targetUserInfo.fcmToken){
    console.error(`Send failed ${targetUser} is registered but missing fcmToken`);
    return res.status(404).send({error: `User exists, but has no token`});
  }
  

  //C) Get token
  const targetToken = targetUserInfo.fcmToken;
  

  // D) --- Construct the message payload ---
  const payload = {
    notification: {
      title: title,
      body: message
    },
    token: targetToken
    //Can add customer data here if needed, will leave stock for now

  }

  // E) --- Send the notification via FCM ---
  admin.messaging().send(payload)
    .then((response) => {
      console.log(`Successfully sent message to '${targetUser}':`, response);
      res.status(200).json({ success: true, messageId: response });
    })
    .catch((error) => {
      console.error(`Error sending message to '${targetUser}':`, error);
      //Handling FCM specific errors
      if(error.code === 'messaging/invalid-registration-token' || error.code === 'messaging/registration-token-not-registered'){
        //remove invalid token from database
        console.log(`Removing invalid token for user '${targetUser}'`);
        delete users[targetUser]; // Remove user from "database"
        return res.status(410).send({ 
          error: "FCM token is invalid or expired. User needs to re-register." 
        });
      }
      res.status(500).json({ error: "Failed to send message", details: error.message });
    });
});

// C) --- Endpoint to get all registered users ---
// This matches the getRegisteredUsers() call in your Java code.
/*app.get('/users', (req, res) => {
  console.log("Fetching list of registered users.");
  // We return the keys of the users object, which are the userIds.
  const userList = Object.keys(users);
  res.status(200).json(userList);
});
*/
//Handle undefined routes
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});


// --- 6. Start the Server ---
const PORT = process.env.PORT || 3000; // You can use any port you like
app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
  console.log('Available endpoints:');
  console.log('  POST   /send           - Send a message');
});

