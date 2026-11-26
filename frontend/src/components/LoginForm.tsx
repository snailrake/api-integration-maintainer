import { useState } from 'react';
import maintainerMark from '../assets/maintainer.svg';
import { Icon } from './Icon';

export function LoginForm() {
  const [visible, setVisible] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  return (
    <section className="auth" aria-labelledby="login-title">
      <div className="brand">
        <img src={maintainerMark} width="34" height="34" alt="" />
        <span>API Maintainer</span>
      </div>
      <div className="auth-content">
        <h1 id="login-title">Вход</h1>
        <form className="auth-form" onSubmit={(event) => {
          event.preventDefault();
          setSubmitted(true);
        }} onInput={() => setSubmitted(false)}>
          <div className="input-group">
            <label htmlFor="login">Логин</label>
            <input id="login" name="login" autoComplete="username" autoCapitalize="none"
              spellCheck={false} required />
          </div>
          <div className="input-group">
            <label htmlFor="password">Пароль</label>
            <div className="password-field">
              <input id="password" name="password" type={visible ? 'text' : 'password'}
                autoComplete="current-password" required />
              <button className="password-toggle" type="button"
                aria-label={visible ? 'Скрыть пароль' : 'Показать пароль'}
                title={visible ? 'Скрыть пароль' : 'Показать пароль'}
                onClick={() => setVisible((previous) => !previous)}>
                <Icon name={visible ? 'eyeOff' : 'eye'} size={19} />
              </button>
            </div>
          </div>
          <button className="sign-in" type="submit">Войти <Icon name="arrow" size={17} /></button>
          <p className="access-help">Забыли пароль? Обратитесь к администратору.</p>
          <p className="auth-feedback" role="status">
            {submitted && 'Демонстрационная форма. Вход доступен в основном приложении.'}
          </p>
        </form>
      </div>
    </section>
  );
}
