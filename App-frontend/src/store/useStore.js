import { create } from 'zustand';
import apiService from '../services/apiService';
import authStorage from '../utils/authStorage';
import { Alert } from 'react-native';

export const useStore = create((set, get) => ({
  persons: [],
  debts: [],
  settlements: [],
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  // Auth
  setAuthenticated: async (token) => {
    if (!token) return;
    await authStorage.setToken(token);
    set({ isAuthenticated: true });
    // Fetch data immediately after authentication
    get().fetchData();
  },

  checkAuth: async () => {
    const token = await authStorage.getToken();
    if (token) {
      set({ isAuthenticated: true });
    } else {
      set({ isAuthenticated: false });
    }
  },

  logout: async () => {
    await authStorage.removeToken();
    set({ 
      isAuthenticated: false, 
      user: null, 
      persons: [], 
      debts: [], 
      settlements: [],
      error: null 
    });
  },

  // Fetching
  fetchData: async () => {
    // 1. Requirement check
    if (!get().isAuthenticated) return;
    
    // 2. State management
    set({ isLoading: true, error: null });
    
    try {
      const [persons, debts, settlements, user] = await Promise.all([
        apiService.getPersons(),
        apiService.getDebts(),
        apiService.getSettlements(),
        apiService.getProfile(),
      ]);
      set({ persons, debts, settlements, user, isLoading: false });
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || 'Network error';
      set({ error: errorMessage, isLoading: false });
      
      // Handle Unauthenticated specifically
      if (err.response && err.response.status === 401) {
        Alert.alert('Session Expired', 'Please log in again.');
        get().logout();
      } else {
        // Only show alert for non-auth errors if we have no data
        if (get().persons.length === 0) {
          Alert.alert('Connection Error', 'Could not sync data with the server. Please check your connection.');
        }
      }
    }
  },

  // Persons
  addPerson: async (payload) => {
    try {
      const newPerson = await apiService.addPerson(payload);
      set((state) => ({ persons: [...state.persons, newPerson] }));
      return newPerson;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to add person';
      set({ error: msg });
      throw err;
    }
  },

  deletePerson: async (id) => {
    try {
      await apiService.deletePerson(id);
      set((state) => ({ persons: state.persons.filter(p => p.id !== id) }));
      // Refetch settlements as they will change after person deletion
      get().fetchData();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to delete person';
      set({ error: msg });
      throw err;
    }
  },

  updateProfilePhone: async (phone) => {
    try {
      const updatedUser = await apiService.updateProfilePhone(phone);
      set({ user: updatedUser });
      return updatedUser;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update phone number';
      set({ error: msg });
      throw err;
    }
  },

  // Debts
  addDebt: async (debtData) => {
    try {
      const newDebt = await apiService.addDebt(debtData);

      // Recording can settle/collapse existing rows on the server (netting a whole
      // relationship), so re-sync debts and settlements from the source of truth
      // rather than appending a single row locally.
      const [debts, settlements] = await Promise.all([
        apiService.getDebts(),
        apiService.getSettlements(),
      ]);
      set({ debts, settlements });
      return newDebt;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to record transaction';
      set({ error: msg });
      throw err;
    }
  },

  acceptDebt: async (id) => {
    try {
      await apiService.acceptDebt(id);
      // Accepting activates the debt and may simplify the ledger - re-sync from server.
      const [debts, settlements] = await Promise.all([
        apiService.getDebts(),
        apiService.getSettlements(),
      ]);
      set({ debts, settlements });
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to accept debt';
      set({ error: msg });
      throw err;
    }
  },

  updateDebt: async (id, debtData) => {
    try {
      const updatedDebt = await apiService.updateDebt(id, debtData);
      set((state) => ({ 
        debts: state.debts.map(d => d.id === id ? updatedDebt : d) 
      }));
      // Update settlements immediately
      const settlements = await apiService.getSettlements();
      set({ settlements });
      return updatedDebt;
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update transaction';
      set({ error: msg });
      throw err;
    }
  },

  deleteDebt: async (id) => {
    try {
      await apiService.deleteDebt(id);
      set((state) => ({ 
        debts: state.debts.filter(d => d.id !== id) 
      }));
      // Update settlements immediately
      const settlements = await apiService.getSettlements();
      set({ settlements });
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to delete transaction';
      set({ error: msg });
      throw err;
    }
  },
}));
