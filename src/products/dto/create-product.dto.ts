import { IsNotEmpty, IsNumber, IsOptional } from 'class-validator';

export class CreateProductDto {
  @IsNotEmpty()
  sku: string;

  @IsNotEmpty()
  name: string;

  @IsOptional()
  category?: string;

  @IsNumber()
  price: number;

  @IsNumber()
  stock: number;
}

