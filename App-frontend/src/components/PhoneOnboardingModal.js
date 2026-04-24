import React, { useState } from 'react';
import { Modal, View, Text, StyleSheet, TextInput, TouchableOpacity, ActivityIndicator, Alert } from 'react-native';
import { Theme } from '../theme/Theme';
import { Phone } from 'lucide-react-native';
import { useStore } from '../store/useStore';

const PhoneOnboardingModal = ({ visible, onComplete }) => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { updateProfilePhone } = useStore();

  const handleSubmit = async () => {
    const cleaned = phoneNumber.replace(/\D/g, '');
    if (cleaned.length !== 10) {
      Alert.alert('Invalid Number', 'Please enter a valid 10-digit phone number.');
      return;
    }

    setIsSubmitting(true);
    try {
      await updateProfilePhone(cleaned);
      onComplete();
    } catch (error) {
      Alert.alert('Error', error.toString() || 'Failed to update phone number.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal visible={visible} transparent={true} animationType="fade">
      <View style={styles.overlay}>
        <View style={styles.content}>
          <View style={styles.iconContainer}>
            <Phone size={32} color={Theme.colors.primary} />
          </View>
          
          <Text style={styles.title}>Complete Your Profile</Text>
          <Text style={styles.subtitle}>
            Please enter your phone number to help your friends find you and settle debts easily.
          </Text>

          <View style={styles.inputContainer}>
            <Text style={styles.prefix}>+91</Text>
            <TextInput
              style={styles.input}
              placeholder="9876543210"
              keyboardType="phone-pad"
              maxLength={10}
              value={phoneNumber}
              onChangeText={setPhoneNumber}
              editable={!isSubmitting}
            />
          </View>

          <TouchableOpacity 
            style={[styles.button, isSubmitting && { opacity: 0.7 }]} 
            onPress={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <ActivityIndicator color={Theme.colors.white} />
            ) : (
              <Text style={styles.buttonText}>Continue</Text>
            )}
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Theme.spacing.lg,
  },
  content: {
    backgroundColor: Theme.colors.white,
    borderRadius: Theme.borderRadius.xl,
    padding: Theme.spacing.xl,
    width: '100%',
    alignItems: 'center',
  },
  iconContainer: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: Theme.colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Theme.spacing.lg,
  },
  title: {
    fontSize: 22,
    fontWeight: '800',
    color: Theme.colors.text,
    marginBottom: Theme.spacing.sm,
  },
  subtitle: {
    fontSize: 14,
    color: Theme.colors.textSecondary,
    textAlign: 'center',
    marginBottom: Theme.spacing.xl,
    lineHeight: 20,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Theme.colors.surface,
    borderRadius: Theme.borderRadius.lg,
    paddingHorizontal: Theme.spacing.md,
    borderWidth: 1,
    borderColor: Theme.colors.border,
    marginBottom: Theme.spacing.xl,
    width: '100%',
    height: 56,
  },
  prefix: {
    fontSize: 16,
    fontWeight: '700',
    color: Theme.colors.text,
    marginRight: Theme.spacing.xs,
  },
  input: {
    flex: 1,
    fontSize: 18,
    color: Theme.colors.text,
    fontWeight: '600',
  },
  button: {
    backgroundColor: Theme.colors.primary,
    width: '100%',
    height: 56,
    borderRadius: Theme.borderRadius.lg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    color: Theme.colors.white,
    fontSize: 16,
    fontWeight: '700',
  },
});

export default PhoneOnboardingModal;
