const registerForm = () => {

    const user = {
        login: '',
        name: '',
        password: ''
    }

    const submit = () => {

        const regDone = resp => {
            $('#userRegModal').modal('close')
            M.toast({html: `User created. You can login now as ${resp.name}!`})
        }

        const regFail = resp => {
            const errors = resp.responseJSON
            if (resp.status === 422 && Array.isArray(errors)) {
                $('#userRegModal input').each((idx, item) => {
                    const failed = errors.some(e => e.hasOwnProperty(item.name))
                    $(item).toggleClass('invalid', failed).toggleClass('valid', !failed)
                })
                errors.forEach(e => M.toast({html: `${Object.keys(e)}: ${Object.values(e)}`}))
            } else {
                M.toast({html: `Error ${resp.status}: ${resp.statusText}`})
            }
        }

        $.post("/public/user/create", JSON.stringify(user)).done(regDone).fail(regFail)
    }

    const showLoginForm = () => {
        $('#userRegModal').modal('close')
        $('#loginModal').modal('open')
    }

    return {
        submit,
        user,
        showLoginForm
    }
}

const loginForm = () => {
    const user = {
        login: '',
        password: ''
    }

    const submit = () => {
        const loginDone = resp => {
            $('#loginModal').modal('close')
            M.toast({html: `Logged in. Hello!`})
            document.dispatchEvent(new CustomEvent('login-event', {}))
        }

        const loginFail = resp => {
            const errors = resp.responseJSON
            if (Array.isArray(errors)) {
                $('#loginModal input').each((idx, item) => {
                    const failed = errors.some(e => e.hasOwnProperty(item.name))
                    $(item).toggleClass('invalid', failed).toggleClass('valid', !failed)
                })
                errors.forEach(e => {
                    if (typeof e === 'string') {
                        M.toast({html: e})
                    } else {
                        M.toast({html: `${Object.keys(e)}: ${Object.values(e)}`})
                    }
                })
            } else {
                M.toast({html: `Error ${resp.status}: ${resp.statusText}`})
            }
        }

        $.post("/public/user/login", JSON.stringify(user)).done(loginDone).fail(loginFail)
    }

    const showRegForm = () => {
        $('#loginModal').modal('close')
        $('#userRegModal').modal('open')
    }

    return {
        submit,
        user,
        showRegForm
    }
}

const userAwareControl = () => {
    return {
        userLoggedIn: false,
        userLoggedOut: true,

        handleLogin() {
            this.userLoggedIn = true
            this.userLoggedOut = false
        },

        init() {
            $.getJSON('/private/user/active').done(this.handleLogin.bind(this))
        }
    }
}
