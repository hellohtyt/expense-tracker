# 💰 Expense Tracker Android App

A comprehensive expense tracking Android application built with **Jetpack Compose** and **Modern Android Architecture** that automatically parses bank SMS messages to track financial transactions.

## ✨ Features

### 🏠 **Home Dashboard**
- Real-time financial overview with income, expenses, and balance
- Recent transactions with detailed information
- Quick SMS sync functionality
- Financial health indicators

### 📊 **Enhanced Financial Insights**
- **Financial Health Score**: AI-powered scoring based on savings rate
- **Previous Period Comparison**: Track changes with percentage indicators
- **Advanced Analytics**: Spending patterns by day and time
- **Interactive Charts**: Income vs Expenses trend visualization
- **Smart Insights**: Personalized financial recommendations
- **Category Breakdown**: Detailed expense analysis with progress bars

### 💳 **Transaction Management**
- Automatic SMS parsing from major Indian banks
- Manual transaction editing and categorization
- Bulk transaction selection and deletion
- Detailed transaction view with original SMS content
- Smart payee and amount detection

### 🏷️ **Category Management**
- Pre-defined and custom categories
- Color-coded category system
- Transaction count per category
- Category-wise expense analysis

### 📱 **Modern UI/UX**
- **Material Design 3** with dynamic theming
- Responsive layouts optimized for all screen sizes
- Smooth animations and transitions
- Intuitive navigation with bottom navigation bar

## 🏗️ **Technology Stack**

### **Frontend**
- **Jetpack Compose**: Modern declarative UI framework
- **Material Design 3**: Latest Material Design components
- **Navigation Compose**: Type-safe navigation

### **Architecture**
- **MVVM Pattern**: Clean separation of concerns
- **Repository Pattern**: Abstracted data layer
- **Dependency Injection**: Hilt for DI
- **Reactive Programming**: Kotlin Flows and Coroutines

### **Data Management**
- **Room Database**: Local SQLite database with type safety
- **Kotlin DateTime**: Modern date/time handling
- **SMS ContentProvider**: Android SMS access

### **Development Tools**
- **Kotlin**: 100% Kotlin codebase
- **Gradle Version Catalogs**: Centralized dependency management
- **KSP**: Kotlin Symbol Processing for better compilation

## 🚀 **Getting Started**

### **Prerequisites**
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34 or higher
- Kotlin 1.9.0 or later
- JDK 17

### **Installation**

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/expense-tracker.git
   cd expense-tracker
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Sync and Build**
   - Let Android Studio sync the project
   - Build the project (`Ctrl+F9` or `Cmd+F9`)

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click Run (`Shift+F10` or `Shift+Cmd+R`)

### **Permissions Required**
- **SMS Read Permission**: To automatically parse bank transaction SMS
- **Storage Permission**: For app data and cache management

## 📱 **App Structure**

```
app/
├── src/main/java/com/expensetracker/
│   ├── data/
│   │   ├── database/        # Room database setup
│   │   ├── dao/            # Data Access Objects
│   │   └── entity/         # Database entities
│   ├── repository/         # Data repositories
│   ├── ui/
│   │   ├── screen/         # Screen composables
│   │   └── component/      # Reusable UI components
│   ├── sms/               # SMS parsing logic
│   └── receiver/          # SMS broadcast receiver
└── build.gradle.kts       # App-level dependencies
```

## 🎯 **Key Features Explained**

### **Automatic SMS Parsing**
- Supports major Indian banks (SBI, HDFC, ICICI, Axis, etc.)
- Regex-based intelligent parsing
- Automatic transaction type detection (Debit/Credit)
- Payee and amount extraction

### **Financial Health Scoring**
- **90+ Score**: 20%+ savings rate (Excellent)
- **75-89 Score**: 10-19% savings rate (Good)
- **60-74 Score**: 5-9% savings rate (Fair)
- **40-59 Score**: 0-4% savings rate (Needs Improvement)
- **<40 Score**: Negative savings (Critical)

### **Advanced Analytics**
- Day-of-week spending patterns
- Time-of-day expense analysis
- Category-wise percentage breakdown
- Previous period comparisons
- Spending frequency calculations

## 🔧 **Configuration**

### **Supported Banks**
The app automatically detects SMS from these banks:
- State Bank of India (SBI)
- HDFC Bank
- ICICI Bank
- Axis Bank
- Kotak Mahindra Bank
- Punjab National Bank (PNB)
- Bank of Baroda
- Canara Bank
- And many more...

### **Custom Categories**
Default categories include:
- Food & Dining
- Transportation
- Shopping
- Entertainment
- Bills & Utilities
- Healthcare
- Education
- Travel
- Investment
- Others

## 📊 **Screenshots**

| Home Screen | Insights | Transactions | Categories |
|-------------|----------|--------------|------------|
| ![Home](screenshots/home.png) | ![Insights](screenshots/insights.png) | ![Transactions](screenshots/transactions.png) | ![Categories](screenshots/categories.png) |

## 🤝 **Contributing**

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### **Development Setup**
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 **Acknowledgments**

- **Jetpack Compose Team** for the amazing declarative UI framework
- **Material Design Team** for the beautiful design system
- **Android Development Community** for continuous inspiration

## 📞 **Support**

If you find this project helpful, please consider:
- ⭐ Starring the repository
- 🐛 Reporting bugs via [Issues](https://github.com/yourusername/expense-tracker/issues)
- 💡 Suggesting features via [Discussions](https://github.com/yourusername/expense-tracker/discussions)

## 🗺️ **Roadmap**

- [ ] **Budget Management**: Set and track category-wise budgets
- [ ] **Export Functionality**: Export data to CSV/PDF
- [ ] **Cloud Sync**: Backup and sync across devices
- [ ] **Investment Tracking**: Track stocks and mutual funds
- [ ] **Bill Reminders**: Smart payment reminders
- [ ] **Receipt Scanner**: OCR-based receipt processing
- [ ] **Multi-language Support**: Localization for Indian languages

---

**Made with ❤️ for better financial management** 