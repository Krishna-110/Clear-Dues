import * as SecureStore from 'expo-secure-store';

const TOKEN_KEY = 'auth_token';

export const authStorage = {
  setToken: async (token) => {
    try {
      await SecureStore.setItemAsync(TOKEN_KEY, token);
    } catch (error) {
      console.error('Error storing the auth token', error);
    }
  },
  getToken: async () => {
    try {
      return await SecureStore.getItemAsync(TOKEN_KEY);
    } catch (error) {
      console.error('Error getting the auth token', error);
    }
  },
  removeToken: async () => {
    try {
      await SecureStore.deleteItemAsync(TOKEN_KEY);
    } catch (error) {
      console.error('Error deleting the auth token', error);
    }
  },
};

export default authStorage;
