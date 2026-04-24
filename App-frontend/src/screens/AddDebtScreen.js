import React, { useState } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, Alert, ScrollView, Modal, FlatList, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useStore } from '../store/useStore';
import { Theme } from '../theme/Theme';
import { Save, UserCircle, ChevronDown, Paperclip } from 'lucide-react-native';

const AddDebtScreen = ({ navigation }) => {
  const { persons, addDebt, fetchData, user } = useStore();
  const allParticipants = user ? [{ ...user, name: 'Myself (You)' }, ...persons] : persons;
  const [debtor, setDebtor] = useState(null);
  const [creditor, setCreditor] = useState(null);
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [showPersonModal, setShowPersonModal] = useState(false);
  const [selectingFor, setSelectingFor] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSave = async () => {
    if (!debtor || !creditor || !amount) {
      Alert.alert('Missing Fields', 'Please select both participants and enter an amount.');
      return;
    }

    if (debtor.phoneNumber === creditor.phoneNumber) {
      Alert.alert('Invalid Selection', 'The payer and the ower cannot be the same person.');
      return;
    }

    if (isNaN(parseFloat(amount))) {
      Alert.alert('Invalid Amount', 'Please enter a valid numeric amount.');
      return;
    }

    setIsSubmitting(true);
    try {
      await addDebt({
        debtorPhone: debtor.phoneNumber,
        creditorPhone: creditor.phoneNumber,
        amount: parseFloat(amount),
        note: note
      });
      fetchData();
      setAmount('');
      setNote('');
      setDebtor(null);
      setCreditor(null);
      Alert.alert('Success', 'Transaction recorded successfully!');
      navigation.navigate('Dashboard');
    } catch (error) {
      Alert.alert('Error', 'Failed to save transaction. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const openPersonPicker = (target) => {
    if (allParticipants.length === 0) {
      Alert.alert('No People Found', 'Please add some people in the "Persons" tab before recording a transaction.');
      return;
    }
    setSelectingFor(target);
    setShowPersonModal(true);
  };

  const selectPerson = (person) => {
    if (selectingFor === 'debtor') setDebtor(person);
    else setCreditor(person);
    setShowPersonModal(false);
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={styles.header}>
          <Text style={styles.title}>Record Transaction</Text>
          <Text style={styles.subtitle}>Enter the details of your shared expense below.</Text>
        </View>

        {/* Form Group */}
        <View style={styles.form}>
          
          {/* Who Paid? (Creditor) */}
          <Text style={styles.label}>Who paid?</Text>
          <TouchableOpacity 
            style={styles.picker} 
            onPress={() => openPersonPicker('creditor')}
            disabled={isSubmitting}
          >
            <UserCircle size={20} color={creditor ? Theme.colors.primary : Theme.colors.textSecondary} />
            <Text style={[styles.pickerText, !creditor && styles.placeholder]}>
              {creditor ? creditor.name : 'Select the person who paid'}
            </Text>
            <ChevronDown size={16} color={Theme.colors.textSecondary} />
          </TouchableOpacity>

          {/* Who Owes? (Debtor) */}
          <Text style={styles.label}>Who owes?</Text>
          <TouchableOpacity 
            style={styles.picker} 
            onPress={() => openPersonPicker('debtor')}
            disabled={isSubmitting}
          >
            <UserCircle size={20} color={debtor ? Theme.colors.primary : Theme.colors.textSecondary} />
            <Text style={[styles.pickerText, !debtor && styles.placeholder]}>
              {debtor ? debtor.name : 'Select the person who owes'}
            </Text>
            <ChevronDown size={16} color={Theme.colors.textSecondary} />
          </TouchableOpacity>

          {/* Amount */}
          <Text style={styles.label}>Amount</Text>
          <View style={styles.inputContainer}>
            <Text style={styles.currencyPrefix}>₹</Text>
            <TextInput
              style={styles.input}
              placeholder="0.00"
              keyboardType="numeric"
              value={amount}
              onChangeText={setAmount}
              editable={!isSubmitting}
            />
          </View>

          {/* Note (Optional) */}
          <Text style={styles.label}>Note / Category (Optional)</Text>
          <View style={styles.inputContainer}>
            <Paperclip size={18} color={Theme.colors.textSecondary} style={{ marginRight: 8 }} />
            <TextInput
              style={styles.input}
              placeholder="e.g. Dinner, Rent, Movies"
              value={note}
              onChangeText={setNote}
              editable={!isSubmitting}
            />
          </View>
        </View>

        <TouchableOpacity 
          style={[styles.saveButton, Theme.shadow.medium, isSubmitting && { opacity: 0.7 }]} 
          onPress={handleSave}
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <ActivityIndicator color={Theme.colors.white} />
          ) : (
            <>
              <Save size={20} color={Theme.colors.white} />
              <Text style={styles.saveButtonText}>Save Transaction</Text>
            </>
          )}
        </TouchableOpacity>
      </ScrollView>

      {/* Person Picker Modal */}
      <Modal visible={showPersonModal} animationType="slide" transparent={true}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Select Person</Text>
            <FlatList
              data={allParticipants}
              keyExtractor={(item) => item.phoneNumber || item.email}
              renderItem={({ item }) => (
                <TouchableOpacity 
                  style={styles.personItem} 
                  onPress={() => selectPerson(item)}
                >
                  <Text style={styles.personName}>{item.name}</Text>
                  <Text style={styles.personEmail}>{item.phoneNumber}</Text>
                </TouchableOpacity>
              )}
              showsVerticalScrollIndicator={false}
            />
            <TouchableOpacity style={styles.closeButton} onPress={() => setShowPersonModal(false)}>
              <Text style={styles.closeButtonText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Theme.colors.background,
  },
  content: {
    padding: Theme.spacing.lg,
  },
  header: {
    marginBottom: Theme.spacing.xl,
  },
  title: {
    fontSize: 28,
    fontWeight: '900',
    color: Theme.colors.text,
    letterSpacing: -1,
  },
  subtitle: {
    fontSize: 16,
    color: Theme.colors.textSecondary,
    marginTop: 4,
  },
  form: {
    marginBottom: Theme.spacing.xl,
  },
  label: {
    fontSize: 14,
    fontWeight: '700',
    color: Theme.colors.text,
    marginBottom: Theme.spacing.xs,
    marginTop: Theme.spacing.md,
  },
  picker: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Theme.colors.surface,
    padding: Theme.spacing.md,
    borderRadius: Theme.borderRadius.lg,
    borderWidth: 1,
    borderColor: Theme.colors.border,
  },
  pickerText: {
    flex: 1,
    marginLeft: Theme.spacing.sm,
    fontSize: 16,
    color: Theme.colors.text,
  },
  placeholder: {
    color: Theme.colors.textSecondary,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Theme.colors.surface,
    padding: Theme.spacing.md,
    borderRadius: Theme.borderRadius.lg,
    borderWidth: 1,
    borderColor: Theme.colors.border,
  },
  currencyPrefix: {
    fontSize: 18,
    fontWeight: '700',
    color: Theme.colors.text,
    marginRight: 8,
  },
  input: {
    flex: 1,
    fontSize: 16,
    color: Theme.colors.text,
  },
  saveButton: {
    backgroundColor: Theme.colors.text,
    flexDirection: 'row',
    height: 56,
    borderRadius: Theme.borderRadius.lg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  saveButtonText: {
    color: Theme.colors.white,
    fontSize: 18,
    fontWeight: '700',
    marginLeft: Theme.spacing.sm,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: Theme.colors.white,
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    padding: Theme.spacing.xl,
    maxHeight: '80%',
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: '800',
    marginBottom: Theme.spacing.lg,
    textAlign: 'center',
  },
  personItem: {
    paddingVertical: Theme.spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: Theme.colors.border + '50',
  },
  personName: {
    fontSize: 16,
    fontWeight: '700',
    color: Theme.colors.text,
  },
  personEmail: {
    fontSize: 12,
    color: Theme.colors.textSecondary,
  },
  closeButton: {
    marginTop: Theme.spacing.lg,
    padding: Theme.spacing.md,
    alignItems: 'center',
  },
  closeButtonText: {
    color: Theme.colors.danger,
    fontWeight: '700',
  },
});

export default AddDebtScreen;
