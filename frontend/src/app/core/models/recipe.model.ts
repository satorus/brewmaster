export interface IngredientDto {
  id: string;
  name: string;
  category: string;
  amount: number;
  unit: string;
  additionTime: string | null;
  notes: string | null;
  sortOrder: number;
}

export interface StepDto {
  id: string;
  stepNumber: number;
  phase: string;
  title: string;
  instructions: string;
  durationMin: number | null;
  targetTempC: number | null;
  timerRequired: boolean;
  notes: string | null;
}

export interface RecipeSummary {
  id: string;
  name: string;
  style: string | null;
  abv: number | null;
  ibu: number | null;
  srm: number | null;
  baseVolumeL: number;
  aiGenerated: boolean;
  createdAt: string;
}

export interface RecipeDetail extends RecipeSummary {
  description: string | null;
  sourceUrl: string | null;
  originalGravity: number | null;
  finalGravity: number | null;
  mashTempC: number | null;
  mashDurationMin: number | null;
  boilDurationMin: number | null;
  fermentationTempC: number | null;
  fermentationDays: number | null;
  notes: string | null;
  createdBy: string;
  updatedAt: string;
  ingredients: IngredientDto[];
  steps: StepDto[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface IngredientRequest {
  name: string;
  category: string;
  amount: number;
  unit: string;
  additionTime?: string;
  notes?: string;
  sortOrder: number;
}

export interface StepRequest {
  stepNumber: number;
  phase: string;
  title: string;
  instructions: string;
  durationMin?: number;
  targetTempC?: number;
  timerRequired: boolean;
  notes?: string;
}

export interface SaveRecipeRequest {
  name: string;
  style?: string;
  description?: string;
  sourceUrl?: string;
  baseVolumeL: number;
  originalGravity?: number;
  finalGravity?: number;
  abv?: number;
  ibu?: number;
  srm?: number;
  mashTempC?: number;
  mashDurationMin?: number;
  boilDurationMin?: number;
  fermentationTempC?: number;
  fermentationDays?: number;
  notes?: string;
  ingredients: IngredientRequest[];
  steps: StepRequest[];
  aiGenerated?: boolean;
}

export interface AiIngredientDto {
  name: string;
  category: string;
  amount: number;
  unit: string;
  additionTime: string | null;
  notes: string | null;
  sortOrder: number;
}

export interface AiStepDto {
  stepNumber: number;
  phase: string;
  title: string;
  instructions: string;
  durationMin: number | null;
  targetTempC: number | null;
  timerRequired: boolean;
  notes: string | null;
}

export interface AiRecipeDto {
  name: string;
  style: string | null;
  description: string | null;
  sourceUrl: string | null;
  baseVolumeL: number;
  originalGravity: number | null;
  finalGravity: number | null;
  abv: number | null;
  ibu: number | null;
  srm: number | null;
  mashTempC: number | null;
  mashDurationMin: number | null;
  boilDurationMin: number | null;
  fermentationTempC: number | null;
  fermentationDays: number | null;
  ingredients: AiIngredientDto[];
  steps: AiStepDto[];
}

export interface AiRecipeSearchResponse {
  recipes: AiRecipeDto[];
}

export interface TasteProfileRequest {
  style?: string;
  bitternessLevel?: number;
  sweetnessLevel?: number;
  colour?: string;
  targetAbvMin?: number;
  targetAbvMax?: number;
  aromaNotes?: string[];
  batchVolumeL: number;
  additionalNotes?: string;
}

export const BEER_STYLES = ['IPA', 'Stout', 'Weizen', 'Lager', 'Saison', 'Pilsner', 'Porter', 'Wheat Beer', 'Sour', 'Other'] as const;
export const BEER_COLOURS = ['Pale', 'Amber', 'Red', 'Dark', 'Black'] as const;
export const AROMA_NOTES = ['Citrus', 'Pine', 'Fruity', 'Malty', 'Roasty', 'Spicy', 'Floral', 'Earthy', 'Tropical', 'Herbal'] as const;

export interface ScaleRequest {
  targetVolumeL: number;
  boilOffRatePercent: number;
  waterToGrainRatio: number;
}

export interface ScaledRecipeResponse {
  recipe: RecipeDetail;
  strikeWaterL: number;
  spargeVolumeL: number;
  preBoilVolumeL: number;
}

export const INGREDIENT_CATEGORIES = ['MALT', 'HOP', 'YEAST', 'ADJUNCT', 'WATER_TREATMENT', 'OTHER'] as const;
export const RECIPE_PHASES = ['PREPARATION', 'MASHING', 'LAUTERING', 'BOILING', 'COOLING', 'FERMENTATION', 'CONDITIONING', 'PACKAGING'] as const;
export const INGREDIENT_UNITS = ['kg', 'g', 'l', 'ml', 'pcs', 'tsp', 'tbsp'] as const;
