export const Theme = {
  colors: {
    primary: '#4F46E5', // Indigo
    secondary: '#10B981', // Emerald
    success: '#10B981', // Success (Match Emerald)
    accent: '#F59E0B', // Amber
    danger: '#EF4444', // Red
    background: '#FFFFFF',
    surface: '#F9FAFB',
    text: '#111827',
    textSecondary: '#6B7280',
    border: '#E5E7EB',
    white: '#FFFFFF',
    card: '#FFFFFF',
    
    // Gradients / Semantic
    owe: '#EF4444',
    get: '#10B981',
    neutral: '#4F46E5',
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
  },
  borderRadius: {
    sm: 8,
    md: 12,
    lg: 16,
    xl: 24,
    full: 9999,
  },
  shadow: {
    light: {
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.05,
      shadowRadius: 10,
      elevation: 2,
    },
    medium: {
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.1,
      shadowRadius: 15,
      elevation: 5,
    },
  }
};
