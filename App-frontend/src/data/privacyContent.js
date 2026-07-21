// Privacy Policy content shown in-app (PrivacyModal) before a user can proceed past
// the Landing screen. Kept as structured data so it renders natively instead of a WebView.
export const PRIVACY_LAST_UPDATED = '2026';

export const PRIVACY_SECTIONS = [
  {
    title: '1. Information We Collect',
    body: 'To facilitate anonymous ledger tracking and peer-to-peer balance matching across loops, we only collect the bare minimum structural data needed to run the network:',
    bullets: [
      { label: 'Account Identifiers', text: 'Tokenized user IDs or basic contact tags used to link transactions.' },
      { label: 'Transaction Records', text: 'Balances logged, liabilities declared, and optimization history.' },
    ],
  },
  {
    title: '2. How Your Data Is Protected',
    body: 'Your privacy is central to our chain architecture. We process transaction lines on an aggregated basis, meaning users involved in a deep chain of settlements do not have visibility into your historical balances or personal financial networks outside of the active optimization loop.',
  },
  {
    title: '3. Data Sharing and Third Parties',
    body: 'Settlement does not sell, trade, or monetize your individual financial transaction data. Because our processing algorithm relies strictly on zero-sum math matrices, we do not require third-party analytical advertising scripts that track your browsing habits.',
  },
  {
    title: '4. Your Rights',
    body: 'You maintain full ownership of your data ledger. You can request to clear your transaction history or delete your node link from our active network databases at any time through the Settings menu inside the application.',
  },
];

export default PRIVACY_SECTIONS;
