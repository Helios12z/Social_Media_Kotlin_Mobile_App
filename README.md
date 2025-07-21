# Vector_SocialMediaMobileApp
 # Vector Social — Kotlin Android Social Media App

Vector Social is a modern, feature-rich **social media application** built using **Kotlin** and **Firebase**, offering a seamless and interactive experience similar to mainstream platforms.  
This app showcases real-time features such as **chatting**, **post sharing**, **reactions**, **voice/video calls**, and an integrated **AI assistant powered by Gemini 1.5 Flash + SerpAPI**.

---

## ✨ Features

### 👤 User Features
- 👥 **Authentication**: Sign up, login, password reset
- 📝 **Profile management**: Edit bio, address, birthday, avatar, gender
- 📚 **Interest selection** at first login

### 📱 Social Feed
- 🖼️ Create and edit posts (with media)
- 💬 Comment, reply, and tag friends
- ❤️ Like/unlike posts and comments
- 🧭 Post privacy (Public / Friends / Private)
- 🔄 Real-time updates and caching
- 🧵 Expand/collapse post content with dynamic layout handling
- Special algorithm for displaying the most suitable posts for particular users

### 💬 Chat System
- 🔔 Real-time 1-1 chat with:
  - 📎 Media (image + link) support
  - 🤖 AI assistant (@VectorAI in group chats)
- 💾 Messages cached locally
- 🧠 AI assistant powered by **Gemini 1.5 Flash + SerpAPI**

### 📞 Calling
- 📞 Voice calls
- 🎥 Video calls using **Google WebRTC**
- ⏰ Handle incoming call notifications even when app is backgrounded

### 📚 Notifications
- 🔔 New comment, mention, like, friend request
- 🧭 Deep links from push notifications to correct screens

### 🔍 Search & Discovery
- 🔎 Search users, posts, friends, and chat threads
- 📄 Post detail navigation
- 🧑‍🤝‍🧑 Friend recommendation and request handling

### 🛡️ Admin
- 🚫 Ban users
- 👮‍♀️ User & comment moderation
- 📈 Analytics-ready backend structure

---

## 🧪 Technical Highlights

| Category                | Tech / Library                                     |
|------------------------|---------------------------------------------------|
| Language               | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white) |
| Backend                | ![Firebase](https://img.shields.io/badge/Firebase-yellow?logo=firebase) |
| AI Assistant           | Gemini 1.5 Flash + SerpAPI                        |
| Realtime Messaging     | Firebase Firestore + Firestore listeners         |
| Authentication         | Firebase Auth                                     |
| Notifications          | OneSignal + FCM                                   |
| UI Design              | Material Design + BlurView (Glassmorphism)       |
| Media Upload           | Imgbb API                                          |
| Voice/Video Call       | WebRTC                                             |
| State Management       | ViewModel + LiveData                              |
| Caching & Paging       | Custom local cache + Paging support               |
| Race Condition         | Firestore Transactions                            |
| Dependency Injection   | ViewModelProvider                                 |

---

## 🧠 AI Assistant

The app includes a friendly assistant named **VectorAI** who can:
- Answer questions in chat
- Be tagged in group chats
- Use Google Gemini + SerpAPI to provide rich responses

---

## 🔐 Handling Concurrency

All race-condition-prone operations such as:
- Liking a post
- Commenting with concurrent edits
are managed using **Firebase Transactions** to ensure **data consistency** and **atomic updates**.
