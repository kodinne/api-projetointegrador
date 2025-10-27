import { Injectable, ConflictException, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { User } from './user.entity';
import { CreateUserDto } from './dto/create-user.dto';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly repo: Repository<User>,
  ) {}

  async create(dto: CreateUserDto): Promise<User> {
    const email = (dto.email || '').toLowerCase().trim();
    const exists = await this.repo.findOne({ where: { email } });
    if (exists) throw new ConflictException('Email already registered');
    const passwordHash = await bcrypt.hash(dto.password, 10);
    const user = this.repo.create({ name: dto.name, email, passwordHash });
    return this.repo.save(user);
  }

  async findByEmail(email: string): Promise<User | null> {
    const e = (email || '').toLowerCase().trim();
    return this.repo.findOne({ where: { email: e } });
  }

  async findById(id: number): Promise<User> {
    const user = await this.repo.findOne({ where: { id } });
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async findAllPublic(): Promise<Pick<User, 'id' | 'name' | 'email'>[]> {
    const users = await this.repo.find({ select: { id: true, name: true, email: true } as any });
    return users as any;
  }
}
