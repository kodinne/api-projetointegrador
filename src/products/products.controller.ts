import { Body, Controller, Get, Param, Patch, Post, Query } from '@nestjs/common';
import { ProductsService } from './products.service';
import { CreateProductDto } from './dto/create-product.dto';

@Controller('products')
export class ProductsController {
  constructor(private readonly products: ProductsService) {}

  @Post()
  create(@Body() dto: CreateProductDto) {
    return this.products.create(dto);
  }

  @Get()
  findAll(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('status') status?: string,
    @Query('q') q?: string,
  ) {
    return this.products.findAll({
      page: parseInt(page || '1', 10),
      limit: Math.min(parseInt(limit || '10', 10), 100),
      status,
      q,
    });
  }

  @Patch(':id/stock/:stock')
  updateStock(@Param('id') id: number, @Param('stock') stock: number) {
    return this.products.updateStock(Number(id), Number(stock));
  }
}
