package productioneventprocessing.DTOs;

import productioneventprocessing.Entity.ProductionEvent;

public record VoidResolved(ProductionEvent voidEvent, ProductionEvent targetCount) {
}
