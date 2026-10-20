import { TranslationResources } from '@xyz/storefront';
import { common as en } from './en/common';
import { custom as enCustom } from './en/custom';
import { common as ja } from './ja/common';
import { custom as jaCustom } from './ja/custom';

/** 標準の文言(common)と自社の部品の文言(custom)を言語ごとにまとめる。 */
export const translations: TranslationResources = {
  ja: { ...ja, ...jaCustom },
  en: { ...en, ...enCustom },
};
