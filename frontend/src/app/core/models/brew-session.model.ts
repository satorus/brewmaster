import { IngredientDto, StepDto } from './recipe.model';

export type Phase =
  | 'PREPARATION' | 'MASHING' | 'LAUTERING' | 'BOILING'
  | 'COOLING' | 'FERMENTATION' | 'CONDITIONING' | 'PACKAGING';

export const PHASE_COLORS: Record<Phase, string> = {
  PREPARATION: '#757575',
  MASHING: '#E65100',
  LAUTERING: '#F9A825',
  BOILING: '#B71C1C',
  COOLING: '#0277BD',
  FERMENTATION: '#2E7D32',
  CONDITIONING: '#00695C',
  PACKAGING: '#4A148C',
};

export interface StepLogEntry {
  stepNumber: number;
  completedAt: string;
  actualTempC?: number;
  notes?: string;
}

export interface BrewSessionResponse {
  id: string;
  recipeId: string;
  eventId?: string;
  volumeL: number;
  currentStep: number;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  notes?: string;
  strikeWaterL: number;
  spargeVolumeL: number;
  preBoilVolumeL: number;
  boilOffRatePercent: number;
  waterToGrainRatio: number;
  startedAt: string;
  completedAt?: string;
  scaledIngredients: IngredientDto[];
  scaledSteps: StepDto[];
  stepLogs: StepLogEntry[];
}

export interface StartSessionRequest {
  recipeId: string;
  eventId?: string;
  targetVolumeL: number;
  boilOffRatePercent?: number;
  waterToGrainRatio?: number;
  notes?: string;
}

export interface AdvanceStepRequest {
  stepNumber: number;
  actualTempC?: number;
  notes?: string;
}
