export interface BestOfferDto {
  shopName: string;
  price: number;
  pricePerUnit: string;
  productUrl: string;
  packageSize: string;
  packagesNeeded: number;
  totalCost: number;
}

export interface AlternativeOfferDto {
  shopName: string;
  price: number;
  productUrl: string;
}

export interface OrderItemDto {
  ingredientName: string;
  requiredAmount: number;
  unit: string;
  searchNote: string | null;
  bestOffer: BestOfferDto | null;
  alternativeOffer: AlternativeOfferDto | null;
}

export interface OrderResultDto {
  orderId: string;
  recipeId: string;
  recipeName: string;
  volumeL: number;
  items: OrderItemDto[];
  estimatedTotalMin: number;
  estimatedTotalMax: number;
  generatedAt: string;
  disclaimer: string;
}

export interface OrderSummaryDto {
  id: string;
  recipeName: string;
  volumeL: number;
  estimatedTotalMin: number;
  estimatedTotalMax: number;
  generatedAt: string;
}

export interface GenerateOrderRequest {
  recipeId: string;
  volumeL: number;
}
