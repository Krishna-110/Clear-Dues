import React, { useEffect, useState } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createStackNavigator } from '@react-navigation/stack';
import { LayoutDashboard, PlusCircle, HandCoins, Users } from 'lucide-react-native';
import { Theme } from './src/theme/Theme';
import * as Linking from 'expo-linking';
import { useStore } from './src/store/useStore';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';

// Screens
import LandingScreen from './src/screens/LandingScreen';
import DashboardScreen from './src/screens/DashboardScreen';
import AddDebtScreen from './src/screens/AddDebtScreen';
import SettlementScreen from './src/screens/SettlementScreen';
import ManagePersonsScreen from './src/screens/ManagePersonsScreen';
import HistoryScreen from './src/screens/HistoryScreen';

const Tab = createBottomTabNavigator();
const Stack = createStackNavigator();

function MainTabs() {
  const insets = useSafeAreaInsets();
  
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ color, size }) => {
          if (route.name === 'Dashboard') return <LayoutDashboard size={size} color={color} />;
          if (route.name === 'Record') return <PlusCircle size={size} color={color} />;
          if (route.name === 'Settlements') return <HandCoins size={size} color={color} />;
          if (route.name === 'Persons') return <Users size={size} color={color} />;
        },
        tabBarActiveTintColor: Theme.colors.primary,
        tabBarInactiveTintColor: Theme.colors.textSecondary,
        tabBarStyle: {
          height: 65 + insets.bottom,
          paddingBottom: insets.bottom > 0 ? insets.bottom : 10,
          paddingTop: 10,
          backgroundColor: Theme.colors.white,
          borderTopWidth: 1,
          borderTopColor: Theme.colors.border,
          ...Theme.shadow.medium,
        },
        headerShown: false,
      })}
    >
      <Tab.Screen name="Dashboard" component={DashboardScreen} />
      <Tab.Screen name="Record" component={AddDebtScreen} />
      <Tab.Screen name="Settlements" component={SettlementScreen} />
      <Tab.Screen name="Persons" component={ManagePersonsScreen} />
    </Tab.Navigator>
  );
}

const LoadingScreen = () => (
  <View style={styles.loadingContainer}>
    <ActivityIndicator size="large" color={Theme.colors.primary} />
  </View>
);

export default function App() {
  const { setAuthenticated, checkAuth, isAuthenticated } = useStore();
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    // 1. Initial Auth Check and initialization
    const init = async () => {
      await checkAuth();
      setIsReady(true);
    };
    init();

    // 2. Handle Deep Linking for OAuth2 Redirects
    const handleDeepLink = (event) => {
      const { url } = event;
      if (url) {
        try {
          const { queryParams } = Linking.parse(url);
          if (queryParams && queryParams.token) {
            setAuthenticated(queryParams.token);
          }
        } catch (error) {
          console.error('Deep link parse error', error);
        }
      }
    };

    const subscription = Linking.addEventListener('url', handleDeepLink);

    // Check for initial URL if app was closed
    Linking.getInitialURL().then((url) => {
      if (url) handleDeepLink({ url });
    });

    return () => {
      subscription.remove();
    };
  }, []);

  if (!isReady) {
    return <LoadingScreen />;
  }

  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          {isAuthenticated ? (
            <>
              <Stack.Screen name="Main" component={MainTabs} />
              <Stack.Screen name="History" component={HistoryScreen} />
            </>
          ) : (
            <Stack.Screen name="Landing" component={LandingScreen} />
          )}
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Theme.colors.white,
  },
});
