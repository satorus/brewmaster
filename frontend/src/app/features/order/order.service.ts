import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { GenerateOrderRequest, OrderResultDto, OrderSummaryDto } from '../../core/models/order.model';
import { PageResponse } from '../../core/models/recipe.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly api = inject(ApiService);

  generate(req: GenerateOrderRequest): Observable<OrderResultDto> {
    return this.api.post<OrderResultDto>('/orders/generate', req);
  }

  list(page = 0, size = 20): Observable<PageResponse<OrderSummaryDto>> {
    return this.api.get<PageResponse<OrderSummaryDto>>('/orders', {
      page: String(page),
      size: String(size)
    });
  }

  getById(id: string): Observable<OrderResultDto> {
    return this.api.get<OrderResultDto>(`/orders/${id}`);
  }
}
