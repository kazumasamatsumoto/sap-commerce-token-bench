import { custom as ja } from '../ja/custom';

/** Texts for custom components (src/app/custom). Keep the same shape as ja/custom.ts (the type below checks it). */
export const custom: typeof ja = {
  xyzShared: {
    ratingLabel: '{{rating}} out of {{max}}',
    quantity: 'Quantity',
    decrease: 'Decrease',
    increase: 'Increase',
  },
  xyzBadges: {
    tier: {
      PREMIUM: 'Premium',
      OUTLET: 'Outlet',
    },
    newArrival: 'New',
    lowStock: 'Few left',
  },
  xyzProductInfo: {
    material: 'Material',
    reviewCount: '{{count}} reviews',
    taxIncluded: 'Tax included',
    policyLabel: 'Shipping and returns',
    freeShipping: 'Free shipping on orders of {{threshold}} or more',
    returns: 'Returns accepted within 7 days of arrival',
    copyLink: 'Copy link to this product',
    copied: 'Link copied',
    copyFailed: 'Could not copy',
  },
  xyzReviews: {
    title: 'Customer reviews',
    empty: 'No reviews yet',
    showMore: 'Show more ({{count}} left)',
    average: 'Average {{average}} ({{count}} reviews)',
    row: '{{stars}} stars: {{count}}',
    sortLabel: 'Review order',
    sort: {
      newest: 'Newest',
      highest: 'Highest rating',
      lowest: 'Lowest rating',
    },
  },
  xyzBrands: {
    title: 'Our brands',
    about: 'About {{name}}',
  },
  xyzCart: {
    freeShippingReached: 'You get free shipping',
    freeShippingRemaining: '{{amount}} more for free shipping',
    freeShippingLabel: 'Progress to free shipping',
    secureCheckout: 'Your payment details are sent encrypted',
    points: 'You earn {{points}} points with this order',
  },
  xyzCheckout: {
    stepsLabel: 'Checkout steps',
    steps: {
      address: 'Address',
      deliveryMode: 'Delivery method',
      payment: 'Payment',
      review: 'Place order',
    },
    helpTitle: 'Need help?',
    helpPhone: 'Customer service 0120-000-000',
    helpHours: 'Weekdays 10:00-18:00',
    nextStepsTitle: 'What happens next',
    nextSteps: {
      mail: 'We will send you an order confirmation email',
      ship: 'We will let you know when your items are shipped',
      contact: 'If you have questions, please contact customer service',
    },
    backToTop: 'Back to the top page',
  },
  xyzContent: {
    bannerCta: 'Learn more',
  },
  xyzFooter: {
    label: 'Footer menu',
    columns: {
      guide: 'Shopping guide',
      support: 'Support',
      company: 'Company',
    },
    links: {
      howToOrder: 'How to order',
      shipping: 'Shipping',
      returns: 'Returns and exchanges',
      contact: 'Contact us',
      faq: 'FAQ',
      about: 'About us',
      careers: 'Careers',
    },
    trustLabel: 'Our promises',
    trust: {
      warranty: { title: '1-year warranty', text: 'One extra year on top of the maker warranty' },
      freeShipping: { title: 'Free shipping over {{threshold}}', text: 'Anywhere in Japan' },
      securePayment: { title: 'Secure payment', text: 'Card details are sent encrypted' },
    },
    paymentTitle: 'Payment methods',
    payment: {
      card: 'Credit card',
      convenience: 'Convenience store',
      bank: 'Bank transfer',
    },
    backToTop: 'Back to top',
    cookie: {
      label: 'About cookies',
      text: 'This site uses cookies to make it easier to use.',
      acceptAll: 'Allow all',
      necessaryOnly: 'Necessary only',
    },
  },
  xyzHeader: {
    noticeLabel: 'Notice',
    notice: 'Customer service is closed during the New Year holidays (Dec 29 - Jan 3)',
    closeNotice: 'Close notice',
    language: 'Language',
    languages: {
      ja: '日本語',
      en: 'English',
    },
  },
  xyzRecentlyViewed: {
    title: 'Recently viewed',
    clear: 'Clear history',
  },
  xyzWishlist: {
    title: 'Wishlist',
    add: 'Add to wishlist',
    remove: 'Remove from wishlist',
    addToCart: 'Add to cart',
    removeShort: 'Remove',
    empty: 'Your wishlist is empty',
    added: '{{name}} was added to the cart',
    close: 'Close',
  },
};
