// Terms & Conditions content shown in-app (TermsModal) before a user can proceed past
// the Landing screen. Kept as structured data so it renders natively instead of a WebView.
export const TERMS_LAST_UPDATED = '2026';

export const TERMS_SECTIONS = [
  {
    title: 'Acceptance of Terms',
    body: 'Your use of the app constitutes acceptance of these terms. If you do not agree, please discontinue use immediately.',
  },
  {
    title: 'Eligibility',
    body: 'You must be at least 18 years old and legally capable of entering into binding agreements to use this app.',
  },
  {
    title: 'User Responsibilities',
    bullets: [
      'You are responsible for the accuracy of debts and records entered.',
      'You agree not to misuse the app for fraudulent or unlawful purposes.',
    ],
  },
  {
    title: 'Limitation of Liability',
    body: 'Settlement App is a facilitation tool. We do not act as a financial institution or guarantee outcomes.',
  },
  {
    title: 'Specific Clause',
    body: 'Settlement App endeavors to facilitate the resolution of all recorded debts; however, we disclaim responsibility for any debts that remain unsettled in cases where no corresponding match is identified.',
  },
];

export default TERMS_SECTIONS;
