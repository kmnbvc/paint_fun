const sharingControl = () => {
    return {
        accessTypes: [],
        selected: '',

        save() {
            const boardId = $('.whiteboard').data('id')
            const done = () => {
                console.log('done')
            }
            const fail = () => {
                console.log('fail')
            }
            if (this.selected.length > 0) {
                $.ajax(`/private/access/set/${boardId}`, {
                    method: 'PUT',
                    data: JSON.stringify(this.selected),
                    contentType: 'application/json'
                }).done(done).fail(fail)
            }
        },

        loadState() {
            const boardId = $('.whiteboard').data('id')
            $.getJSON(`/private/access/state/${boardId}`).then(data => {
                const types = data.map(value => value[0])
                this.accessTypes.push(...types)
                this.selected = (data.find(value => value[1]) || [''])[0]
            })
        }
    }
}
