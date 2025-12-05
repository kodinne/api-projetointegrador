import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
  ParseIntPipe,
} from '@nestjs/common';
import { ProductsService } from './products.service';
import { CreateProductDto } from './dto/create-product.dto';

@Controller('products')
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Post()
  create(@Body() dto: CreateProductDto) {
    return this.productsService.create(dto);
  }

  @Get()
  findAll(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('status') status?: string,
    @Query('q') q?: string,
  ) {
    return this.productsService.findAll({
      page: parseInt(page || '1', 10),
      limit: Math.min(parseInt(limit || '10', 10), 100),
      status,
      q,
    });
  }

  // opcional: buscar por id (útil para front)
  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.productsService.findOne(id);
  }

  @Patch(':id/stock/:stock')
  updateStock(
    @Param('id', ParseIntPipe) id: number,
    @Param('stock', ParseIntPipe) stock: number,
  ) {
    return this.productsService.updateStock(id, stock);
  }

  // === ROTA DELETE ADICIONADA ===
  @Delete(':id')
  remove(@Param('id', ParseIntPipe) id: number) {
    return this.productsService.remove(id);
  }
}
