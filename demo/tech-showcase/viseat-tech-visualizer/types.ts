export enum SceneType {
  INTRO = 'INTRO',
  DIFFERENTIAL = 'DIFFERENTIAL',
  DYNAMIC = 'DYNAMIC',
  FUSION = 'FUSION',
  PERSONALIZATION = 'PERSONALIZATION'
}

export interface FoodItem {
  id: string;
  name: string;
  calories: number;
  weight: number; // grams
  hidden?: boolean;
  consumed?: boolean;
}

export interface UserProfile {
  id: string;
  name: string;
  condition: string;
  goal: string;
  color: string;
}
