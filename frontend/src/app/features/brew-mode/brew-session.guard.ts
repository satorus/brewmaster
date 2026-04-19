import { CanDeactivateFn } from '@angular/router';

export interface HasCanDeactivate {
  canDeactivate(): boolean;
}

export const brewSessionGuard: CanDeactivateFn<HasCanDeactivate> = (component) =>
  component.canDeactivate() || confirm('Leave brew session? Your progress is saved and you can return.');
