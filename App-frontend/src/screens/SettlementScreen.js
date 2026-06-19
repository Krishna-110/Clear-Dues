import React, { useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useStore } from '../store/useStore';
import { Theme } from '../theme/Theme';
import { HandCoins, Sparkles, CheckCircle2 } from 'lucide-react-native';
import TransactionCard from '../components/TransactionCard';

const SettlementScreen = () => {
  const { settlements, fetchData, isLoading, user } = useStore();

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const displayedSettlements = settlements.filter(
    s => s.fromPhone === user?.phoneNumber || s.toPhone === user?.phoneNumber
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <View style={styles.titleRow}>
          <HandCoins size={28} color={Theme.colors.primary} />
          <Text style={styles.headerTitle}>Optimized Settlements</Text>
        </View>
        <Text style={styles.subtitle}>
          Proposed transactions to square all pending accounts globally.
        </Text>
      </View>



      <FlatList
        data={displayedSettlements}
        keyExtractor={(item, index) => index.toString()}
        renderItem={({ item }) => (
          <View style={styles.settlementItem}>
            <TransactionCard
              debtor={item?.from || 'Unknown'}
              creditor={item?.to || 'Unknown'}
              amount={item?.amount || 0}
            />
            <View style={styles.actionPrompt}>
              <Sparkles size={12} color={Theme.colors.secondary} />
              <Text style={styles.promptText}>
                {item?.from} needs to pay {item?.to} ₹{item?.amount}
              </Text>
            </View>
          </View>
        )}
        contentContainerStyle={styles.list}
        refreshControl={
          <RefreshControl refreshing={isLoading} onRefresh={fetchData} tintColor={Theme.colors.primary} />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <View style={styles.successIconOuter}>
               <CheckCircle2 size={60} color={Theme.colors.secondary} />
            </View>
            <Text style={styles.emptyText}>All debts are optimized and settled!</Text>
            <Text style={styles.emptySubtext}>Your accounts are currently squared up. To settle a due, record the repayment in the Record tab.</Text>
          </View>
        }
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Theme.colors.background,
  },
  header: {
    padding: Theme.spacing.lg,
    paddingBottom: Theme.spacing.md,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  headerTitle: {
    fontSize: 26,
    fontWeight: '900',
    color: Theme.colors.text,
    marginLeft: Theme.spacing.sm,
    letterSpacing: -1,
  },
  subtitle: {
    fontSize: 16,
    color: Theme.colors.textSecondary,
    lineHeight: 22,
  },
  successIconOuter: {
    marginBottom: Theme.spacing.md,
    backgroundColor: Theme.colors.secondary + '15',
    padding: Theme.spacing.lg,
    borderRadius: 50,
  },
  list: {
    paddingBottom: Theme.spacing.xl,
  },
  settlementItem: {
    marginBottom: Theme.spacing.md,
  },
  actionPrompt: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Theme.spacing.lg,
    marginTop: -Theme.spacing.xs,
  },
  promptText: {
    fontSize: 12,
    fontWeight: '600',
    color: Theme.colors.textSecondary,
    marginLeft: 6,
    fontStyle: 'italic',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 100,
    paddingHorizontal: Theme.spacing.xl,
  },
  emptyText: {
    fontSize: 20,
    fontWeight: '800',
    color: Theme.colors.text,
    textAlign: 'center',
    marginBottom: Theme.spacing.sm,
  },
  emptySubtext: {
    fontSize: 14,
    color: Theme.colors.textSecondary,
    textAlign: 'center',
  },
});

export default SettlementScreen;
