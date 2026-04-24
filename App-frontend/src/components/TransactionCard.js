import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Theme } from '../theme/Theme';
import { MoveRight } from 'lucide-react-native';

const TransactionCard = ({ debtor, creditor, amount, status, note }) => {
  const debtorName = debtor || 'Unknown';
  const creditorName = creditor || 'Unknown';
  const amountValue = amount || 0;
  const isSettled = status === 'SETTLED';

  return (
    <View style={[styles.card, Theme.shadow.light, isSettled && styles.settledCard]}>
      <View style={styles.personBlock}>
        <View style={[styles.avatar, isSettled && styles.settledAvatar]}>
          <Text style={[styles.avatarText, isSettled && styles.settledText]}>{debtorName.charAt(0).toUpperCase()}</Text>
        </View>
        <View style={styles.details}>
          <Text style={[styles.name, isSettled && styles.settledText]} numberOfLines={1}>{debtorName}</Text>
          <Text style={styles.label}>{isSettled ? 'Owed' : 'Owes'}</Text>
        </View>
      </View>
      
      <View style={styles.connector}>
        <Text style={[styles.amount, isSettled && styles.settledText]}>₹{amountValue}</Text>
        {isSettled ? (
          <View style={styles.settledBadge}>
            <Text style={styles.settledBadgeText}>SETTLED</Text>
          </View>
        ) : (
          <View style={styles.pendingBadge}>
            <Text style={styles.pendingBadgeText}>PENDING</Text>
          </View>
        )}
      </View>

      <View style={[styles.personBlock, { alignItems: 'flex-end' }]}>
        <View style={[styles.avatar, { backgroundColor: Theme.colors.secondary + '15' }, isSettled && styles.settledAvatar]}>
          <Text style={[styles.avatarText, { color: Theme.colors.secondary }, isSettled && styles.settledText]}>{creditorName.charAt(0).toUpperCase()}</Text>
        </View>
        <View style={[styles.details, { alignItems: 'flex-end' }]}>
          <Text style={[styles.name, isSettled && styles.settledText]} numberOfLines={1}>{creditorName}</Text>
          <Text style={styles.label}>{isSettled ? 'Recvd' : 'Gets'}</Text>
        </View>
      </View>
      
      {note && (
        <View style={styles.noteOverlay}>
          <Text style={styles.noteText}>{note}</Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: Theme.colors.white,
    padding: Theme.spacing.md,
    borderRadius: Theme.borderRadius.xl,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Theme.spacing.sm,
    marginHorizontal: Theme.spacing.md,
    borderWidth: 1,
    borderColor: Theme.colors.border + '30',
  },
  personBlock: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Theme.colors.primary + '15',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
  },
  avatarText: {
    fontSize: 14,
    fontWeight: '700',
    color: Theme.colors.primary,
  },
  details: {
    flex: 1,
  },
  name: {
    fontSize: 14,
    fontWeight: '700',
    color: Theme.colors.text,
  },
  label: {
    fontSize: 10,
    color: Theme.colors.textSecondary,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  connector: {
    alignItems: 'center',
    paddingHorizontal: Theme.spacing.sm,
    minWidth: 80,
  },
  amount: {
    fontSize: 14,
    fontWeight: '800',
    color: Theme.colors.text,
    marginBottom: 2,
  },
  settledCard: {
    backgroundColor: Theme.colors.background,
    borderColor: Theme.colors.border,
    opacity: 0.8,
  },
  settledAvatar: {
    backgroundColor: Theme.colors.border,
  },
  settledText: {
    color: Theme.colors.textSecondary,
    textDecorationLine: 'line-through',
  },
  settledBadge: {
    backgroundColor: Theme.colors.border,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    marginTop: 2,
  },
  settledBadgeText: {
    fontSize: 8,
    fontWeight: '900',
    color: Theme.colors.textSecondary,
  },
  pendingBadge: {
    backgroundColor: Theme.colors.primary + '20',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    marginTop: 2,
    borderWidth: 0.5,
    borderColor: Theme.colors.primary + '40',
  },
  pendingBadgeText: {
    fontSize: 8,
    fontWeight: '900',
    color: Theme.colors.primary,
  },
  noteOverlay: {
    position: 'absolute',
    bottom: -8,
    right: 12,
    backgroundColor: Theme.colors.surface,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Theme.colors.border + '50',
  },
  noteText: {
    fontSize: 9,
    fontWeight: '600',
    color: Theme.colors.textSecondary,
    fontStyle: 'italic',
  },
});

export default TransactionCard;
