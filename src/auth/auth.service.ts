import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { UsersService } from '../users/users.service';

@Injectable()
export class AuthService {
  constructor(private users: UsersService, private jwt: JwtService) {}

  async validateUser(email: string, password: string) {
    const user = await this.users.findByEmail((email || '').toLowerCase());
    if (!user) throw new UnauthorizedException('Invalid credentials');
    const pass = password ?? '';
    const hash = user.passwordHash ?? '';
    if (!pass || !hash) throw new UnauthorizedException('Invalid credentials');
    const ok = await bcrypt.compare(pass, hash);
    if (!ok) throw new UnauthorizedException('Invalid credentials');
    return user;
  }

  async login(email: string, password: string) {
    const user = await this.validateUser(email, password);
    const payload = { sub: user.id, email: user.email, name: user.name };
    return { access_token: this.jwt.sign(payload), user: payload };
  }
}
