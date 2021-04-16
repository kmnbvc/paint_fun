'use strict';

(function () {

    const send = function (data) {
        const whiteboardId = $('.whiteboard').data('id')
        connection.send(JSON.stringify({whiteboardId, data}))
    }

    const connectionUrl = function () {
        const id = $('.whiteboard').data('id')
        return "ws://localhost:9000/ws/" + id
    }

    const connection = new WebSocket(connectionUrl())

    connection.onopen = function (event) {
        console.log("ws connection established", event)
    }

    connection.onerror = function (error) {
        console.log('WebSocket Error ', error)
    }

    connection.onmessage = function (event) {
        onDrawingEvent(JSON.parse(event.data).data)
    }

    const canvas = document.getElementById('wb-canvas')
    const colors = document.getElementsByClassName('color')
    const context = canvas.getContext('2d')

    const current = {
        color: 'black'
    }
    let drawing = false

    canvas.addEventListener('mousedown', onMouseDown, false)
    canvas.addEventListener('mouseup', onMouseUp, false)
    canvas.addEventListener('mouseout', onMouseUp, false)
    canvas.addEventListener('mousemove', throttle(onMouseMove, 10), false)

    canvas.addEventListener('touchstart', onMouseDown, false)
    canvas.addEventListener('touchend', onMouseUp, false)
    canvas.addEventListener('touchcancel', onMouseUp, false)
    canvas.addEventListener('touchmove', throttle(onMouseMove, 10), false)

    for (let i = 0; i < colors.length; i++) {
        colors[i].addEventListener('click', onColorUpdate, false)
    }

    window.addEventListener('resize', onResize, false)
    onResize()


    function drawLine(x0, y0, x1, y1, color, emit) {
        context.beginPath()
        context.moveTo(x0, y0)
        context.lineTo(x1, y1)
        context.strokeStyle = color
        context.lineWidth = 2
        context.stroke()
        context.closePath()

        if (!emit) {
            return
        }
        const w = canvas.width
        const h = canvas.height

        send({
            x0: x0 / w,
            y0: y0 / h,
            x1: x1 / w,
            y1: y1 / h,
            color: color
        })
    }

    function onMouseDown(e) {
        drawing = true
        current.x = e.clientX || e.touches[0].clientX
        current.y = e.clientY || e.touches[0].clientY
    }

    function onMouseUp(e) {
        if (!drawing) {
            return
        }
        drawing = false
        drawLine(current.x, current.y, e.clientX || e.touches[0].clientX, e.clientY || e.touches[0].clientY, current.color, true)
    }

    function onMouseMove(e) {
        if (!drawing) {
            return
        }
        drawLine(current.x, current.y, e.clientX || e.touches[0].clientX, e.clientY || e.touches[0].clientY, current.color, true)
        current.x = e.clientX || e.touches[0].clientX
        current.y = e.clientY || e.touches[0].clientY
    }

    function onColorUpdate(e) {
        current.color = e.target.className.split(' ')[1]
    }

    function throttle(callback, delay) {
        let previousCall = new Date().getTime()
        return function () {
            let time = new Date().getTime()

            if ((time - previousCall) >= delay) {
                previousCall = time
                callback.apply(null, arguments)
            }
        }
    }

    function onDrawingEvent(data) {
        const w = canvas.width
        const h = canvas.height
        drawLine(data.x0 * w, data.y0 * h, data.x1 * w, data.y1 * h, data.color)
    }

    function onResize() {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
    }
})()

$(document).ready(function () {
    M.AutoInit()
})

const registerForm = () => {

    const user = {
        login: '',
        name: '',
        password: ''
    }

    const submit = () => {

        const regDone = resp => {
            $('#userRegModal').modal('close')
            M.toast({html: `User created. Hello ${resp.name}!`})
        }

        const regFail = resp => {
            const errors = resp.responseJSON
            if (resp.status === 422 && typeof errors !== 'undefined') {
                $('#userRegModal input').each((idx, item) => {
                    const failed = typeof errors[item.name] !== 'undefined'
                    $(item).toggleClass('invalid', failed).toggleClass('valid', !failed)
                    if (failed) M.toast({html: `${item.name}: ${errors[item.name]}`})
                })
            } else {
                M.toast({html: `Error ${resp.status}: ${resp.statusText}`})
            }
        }

        $.post("/user/create", JSON.stringify(user)).done(regDone).fail(regFail)
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
            M.toast({html: `Error ${resp.status}: ${resp.statusText}`})
        }

        $.post("/user/login", JSON.stringify(user)).done(loginDone).fail(loginFail)
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
            $.getJSON('/user/active').done(this.handleLogin.bind(this))
        }
    }
}
