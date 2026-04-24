import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Theme } from '../theme/Theme';
import { ArrowDownLeft, ArrowUpRight, Wallet } from 'lucide-react-native';

const SummaryCard = ({ title, amount, type }) => {
  const isPositive = type === 'get';
  const isNegative = type === 'owe';

  const color = isPositive ? Theme.colors.get : isNegative ? Theme.colors.owe : Theme.colors.neutral;
  const Icon = isPositive ? ArrowDownLeft : isNegative ? ArrowUpRight : Wallet;

  return (
    <View style={[styles.card, Theme.shadow.light]}>
      <View style={[styles.iconContainer, { backgroundColor: color + '15' }]}>
        <Icon size={20} color={color} />
      </View>
      <View style={styles.content}>
        <Text style={styles.title}>{title}</Text>
        <Text style={[styles.amount, { color }]}>
          ₹{Math.abs(amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: Theme.colors.white,
    padding: Theme.spacing.md,
    borderRadius: Theme.borderRadius.xl,
    flex: 1,
    marginHorizontal: Theme.spacing.xs,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Theme.colors.border + '50',
  },
  iconContainer: {
    width: 40,
    height: 40,
    borderRadius: Theme.borderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Theme.spacing.sm,
  },
  content: {
    flex: 1,
  },
  title: {
    fontSize: 11,
    color: Theme.colors.textSecondary,
    marginBottom: 2,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  amount: {
    fontSize: 16,
    fontWeight: '800',
  },
});

export default SummaryCard;
